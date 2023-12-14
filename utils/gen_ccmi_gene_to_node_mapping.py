#! /usr/bin/env python

import os
import sys
import getpass
import argparse
import json
import logging

from biothings_client import get_client

from ndex2.client import Ndex2
from ndex2.cx2 import RawCX2NetworkFactory
from ndex2 import constants


class Formatter(argparse.ArgumentDefaultsHelpFormatter,
                argparse.RawDescriptionHelpFormatter):
    pass


LOGGER = logging.getLogger(__name__)
LOG_FORMAT = "%(asctime)-15s %(levelname)s %(relativeCreated)dms " \
             "%(filename)s::%(funcName)s():%(lineno)d %(message)s"

NETWORK_TO_GENE_TO_NODE_KEY = 'networkToGeneToNodeMap'


def _setup_logging(args):
    """
    Sets up logging based on parsed command line arguments.
    If args.logconf is set use that configuration otherwise look
    at args.verbose and set logging for this module and the one
    in ndexutil specified by TSV2NICECXMODULE constant
    :param args: parsed command line arguments from argparse
    :raises AttributeError: If args is None or args.logconf is None
    :return: None
    """

    if args.logconf is None:
        level = (50 - (10 * args.verbose))
        logging.basicConfig(format=LOG_FORMAT,
                            level=level)
        LOGGER.setLevel(level)
        return

    # logconf was set use that file
    logging.config.fileConfig(args.logconf,
                              disable_existing_loggers=False)


def _parse_arguments(desc, args):
    parser = argparse.ArgumentParser(description=desc,
                                     formatter_class=Formatter)
    parser.add_argument('inputdbresults', help='Input databaseresults.json or'
                                               'dbresults.json file from'
                                               'pathway relevance or enrichment'
                                               'REST service')
    parser.add_argument('outputdbresults', help='Output dbresults.json file')
    parser.add_argument('--networksets', nargs='+', type=str,
                        default=['c2228290-368e-11ec-b3be-0ac135e8bacf',
                                 'df8574a2-909b-11ee-8a13-005056ae23aa'],
                        help='NDEx networkset IDs of CCMI networks to update'
                             'mapping on')
    parser.add_argument('--verbose', '-v', action='count', default=0,
                        help='Increases verbosity of logger to standard '
                             'error for log messages in this module and '
                             '. Messages are '
                             'output at these python logging levels '
                             '-v = ERROR, -vv = WARNING, -vvv = INFO, '
                             '-vvvv = DEBUG, -vvvvv = NOTSET (default is to '
                             'log CRITICAL)')
    parser.add_argument('--logconf', default=None,
                        help='Path to python logging configuration file in '
                             'format consumable by fileConfig. See '
                             'https://docs.python.org/3/library/logging.html '
                             'for more information. '
                             'Setting this overrides -v|--verbose parameter '
                             'which uses default logger. (default None)')
    parser.add_argument('--tmpdir', default=None, help='Specifies alternate '
                                                       'temp directory'
                                                       ' to use')
    return parser.parse_args(args)


def get_ndex_client(dbresults, networkset_id=None):
    """
    Gets connection to NDEx server by looking for connection
    credentials in **dbresults** who have networkset id matching **networkset_id**

    :param dbresults: NDEx Enrichment database creation configuration
    :type dbresults: dict
    :param networkset_id: Network set
    :type networkset_id: str
    :return: connection to NDEx server or ``None`` if there
             was no matching configuration
    :rtype: :py:class:`ndex2.client.Ndex2`
    """
    if networkset_id not in dbresults['databaseConnectionMap']:
        return None
    con_params = dbresults['databaseConnectionMap'][networkset_id]
    return Ndex2(host=con_params['server'], username=con_params['user'],
                 password=con_params['password'])


def load_dbresults_and_populate_networkids(inputfile):
    """
    Loads dbresults into json object and adds
    networkSetId to each result by examining 'databaseConnectionMap'
    :param source_client:
    :param inputfile:
    :return:
    """
    LOGGER.info('Loading input databaseresults')
    with open(inputfile, 'r') as f:
        dbresults = json.load(f)
    uuid_to_netset = {}
    for ckey in dbresults['databaseConnectionMap'].keys():
        con_entry = dbresults['databaseConnectionMap'][ckey]
        uuid_to_netset[ckey] = con_entry['networkSetId']

    for entry in dbresults['results']:
        if entry['uuid'] not in uuid_to_netset:
            sys.stderr.write(entry['uuid'] +
                             ' not in databaseConnectionMap. Skipping...\n')
            continue
        entry['networkSetId'] = uuid_to_netset[entry['uuid']]
    return dbresults


def update_nodemappings(source_client=None, dbresults=None,
                        networkset_id=None):
    """
    Finds networkset_id in dbresults and loads each network, performing mapping
    of genes to node ids

    :param source_client:
    :param dbresults:
    :return:
    """
    cx2fac = RawCX2NetworkFactory()
    for entry in dbresults['results']:
        if entry['networkSetId'] != networkset_id:
            continue
        res = source_client.get_networkset(entry['networkSetId'])
        num_networks = len(res['networks'])
        LOGGER.info('There are ' + str(num_networks) + ' in CCMI networkset_id')
        for net_id in res['networks']:
            LOGGER.info('Downloading network: ' + str(net_id))
            client_resp = source_client.get_network_as_cx2_stream(net_id)
            cx2_network = cx2fac.get_cx2network(client_resp.json())
            LOGGER.debug('Loaded: ' + str(cx2_network.get_name()))
            dbresults[NETWORK_TO_GENE_TO_NODE_KEY][net_id] = get_nodemapping_for_network(cx2_network=cx2_network)


def get_attribute_value_from_node(node_obj=None,
                                  uniprot_cols=['Bait ID', 'Bait', 'r', 'represents',
                                                'BaitUniprot', 'Prey', 'PreyUniprot',
                                                'human (common)']):
    """
    Gets value for 1st attribute encountered in **uniprot_cols** list

    :param node_obj: CX2 node json fragment
    :type node_obj: dict
    :param uniprot_cols: List of attribute names
    :type uniprot_cols: list
    :return:
    """
    for col in uniprot_cols:
        if col in node_obj['v']:
            return node_obj['v'][col]
    return None


def get_updated_nodemap_gene_names(symbolmap=None,):
    """
    Queries mygene to get the symbol for gene symbols and uniprot
    ids found in **symbolmap** passed in

    :param symbolmap: mapping of symbol => [ node ids ]
    :type symbolmap: dict
    :return: Map updated with new symbols aka newsymbol => [ node ids ]
    :rtype: dict
    """
    symbol_list = []
    for s in set(symbolmap.keys()):
        symbol_list.append(s)

    mg = get_client("gene")
    res = mg.querymany(symbol_list, species='human',
                       scopes=['symbol', 'uniprot'],
                       fields=['symbol'])
    node_map = {}
    for entry in res:
        if 'symbol' not in entry:
            continue
        symbol = entry['symbol']
        if symbol not in node_map:
            node_map[symbol] = []
        if entry['query'] in symbolmap:
            node_map[symbol].extend(symbolmap[entry['query']])

    return node_map


def get_nodemapping_for_network(cx2_network=None):
    """

    :param cx2_network:
    :return:
    """
    nodes_dict = cx2_network.get_nodes()
    if nodes_dict is None:
        LOGGER.warning('Network ' + str(cx2_network.get_name()) +
                       ' does not have any nodes!!!')
        return []

    temp_nodemap = {}
    for key in cx2_network.get_nodes().keys():
        node_obj = cx2_network.get_node(key)
        if 'v' not in node_obj:
            continue
        if 'NodeType' in node_obj['v'] and node_obj['v']['NodeType'] == 'drug':
            LOGGER.debug('Skipping ' + str(node_obj) + ' because it is of type drug')
            continue
        temp_symbol = None
        uniprot_val = get_attribute_value_from_node(node_obj)
        if uniprot_val is not None:
            temp_symbol = uniprot_val
            if ':' in uniprot_val:
                temp_symbol = uniprot_val[uniprot_val.index(':') + 1:].upper()

        # todo need to check for both n and name
        if temp_symbol is None:
            if 'n' in node_obj['v']:
                temp_symbol = node_obj['v']['n'].upper()
            elif 'name' in node_obj['v']:
                temp_symbol = node_obj['v']['name'].upper()
            if ':' in temp_symbol:
                temp_symbol = temp_symbol[temp_symbol.index(':')+1:].upper()

        if temp_symbol is None:
            LOGGER.debug('Couldnt get a symbol for: ' + str(node_obj))
            continue
        if temp_symbol not in temp_nodemap:
            temp_nodemap[temp_symbol] = []

        if key not in temp_nodemap[temp_symbol]:
            temp_nodemap[temp_symbol].append(key)

    if LOGGER.isEnabledFor(logging.DEBUG):
        LOGGER.debug('Raw mapping for ' + str(cx2_network.get_name()) +
                     ' ' + str(temp_nodemap))
    node_map = get_updated_nodemap_gene_names(symbolmap=temp_nodemap)
    if LOGGER.isEnabledFor(logging.DEBUG):
        LOGGER.debug('Updated mapping for ' + str(cx2_network.get_name()) +
                     ' ' + str(node_map))
        compare_mappings(temp_nodemap, node_map)
    return node_map


def compare_mappings(orig_nodemap, updated_nodemap):
    """

    :param orig_nodemap:
    :param updated_nodemap:
    :return:
    """
    orig_inverted = invert_mapping(orig_nodemap)
    update_inverted = invert_mapping(updated_nodemap)
    for key in orig_inverted.keys():
        if key not in update_inverted:
            continue
        if orig_inverted[key] != update_inverted[key]:
            LOGGER.debug('Mapping ' + str(orig_inverted[key]) + ' => ' + str(update_inverted[key]))


def invert_mapping(nodemap):
    """

    :param nodemap:
    :return:
    """
    inverted_mapping = {}
    for key in nodemap.keys():
        for entry in nodemap[key]:
            if entry not in inverted_mapping:
                inverted_mapping[entry] = key
    return inverted_mapping


def write_output_dbresults(theargs=None, dbresults=None):
    with open(theargs.outputdbresults, 'w') as f:
        json.dump(dbresults, f, indent=2)
    sys.stdout.write('\ndbresults.json file that can be used as input for Pathway relevance'
                     ' saved here: ' + theargs.outputdbresults + '\n\n')


def update_mapping(theargs):
    """

    :param theargs:
    :return:
    """
    sys.stdout.write('\nConverting database\n\n')
    dbresults = load_dbresults_and_populate_networkids(theargs.inputdbresults)
    if NETWORK_TO_GENE_TO_NODE_KEY not in dbresults:
        dbresults[NETWORK_TO_GENE_TO_NODE_KEY] = {}

    for ns in theargs.networksets:
        LOGGER.info('Processing networkset: ' + ns)
        source_client = get_ndex_client(dbresults,
                                        networkset_id=ns)
        update_nodemappings(source_client=source_client,
                            networkset_id=ns,
                            dbresults=dbresults)

    write_output_dbresults(theargs, dbresults)
    sys.stdout.write('\nProcessing complete. Have a nice day.\n\n')
    return 0


def main(arglist):
    desc = """
              This program takes an input dbresults.json/databaseresults.json
              and manually generates "{network_to_gene_to_node_key}" mapping for
              CCMI networks in networksets passed in via --networksets flag. 
              This is done by examining the nodes looking for Bait ID, 
              BaitUniprot, Prey, PreyUniprot, or Represents attributes to 
              get the uniprot id. If that fails the name is used as gene 
              symbol which is then fed to mygene to update with latest gene 
              symbol.
              
              To use an account on the source NDEx server must exist which 
              has access to all networks in networkset XXX input 
              dbresults.json file
    """.format(network_to_gene_to_node_key=NETWORK_TO_GENE_TO_NODE_KEY)
    theargs = _parse_arguments(desc, arglist[1:])
    _setup_logging(theargs)

    return update_mapping(theargs)


if __name__ == '__main__':  # pragma: no cover
    sys.exit(main(sys.argv))


