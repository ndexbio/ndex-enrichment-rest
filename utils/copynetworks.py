#! /usr/bin/env python

import os
import sys
import getpass
import argparse
import json
import logging
import tempfile
import shutil

from ndex2.client import Ndex2


class Formatter(argparse.ArgumentDefaultsHelpFormatter,
                argparse.RawDescriptionHelpFormatter):
    pass


LOGGER = logging.getLogger(__name__)
LOG_FORMAT = "%(asctime)-15s %(levelname)s %(relativeCreated)dms " \
             "%(filename)s::%(funcName)s():%(lineno)d %(message)s"

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


def get_client(server, username, thepassword):
    """
    Gets Ndex2 python client

    :param server:
    :param username:
    :param thepassword:
    :return:
    """
    return Ndex2(server, username, thepassword)


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


def update_dbresults_with_networks_for_each_result(source_client=None, dbresults=None):
    totalnetworks = 0
    for entry in dbresults['results']:
        res = source_client.get_networkset(entry['networkSetId'])
        entry['networks'] = res['networks']
        num_networks = len(entry['networks'])
        totalnetworks += num_networks
        LOGGER.debug('For ' + entry['name'] + ' found ' +
                     str(num_networks) + ' networks')
    LOGGER.info('In ' + str(len(dbresults['results'])) +
                ' databases found ' + str(totalnetworks) +
                ' networks to download')


def write_output_dbresults(theargs=None, dbresults=None):
    with open(theargs.outputdbresults, 'w') as f:
        json.dump(dbresults, f, indent=2)
    sys.stdout.write('\ndbresults.json file that can be used as input for Pathway relevance'
                     ' saved here: ' + theargs.outputdbresults + '\n\n')


def download_network(tmpdir=None, source_client=None, network_id=None):
    """

    :param source_client:
    :param network_id:
    :return:
    """
    net_path = os.path.join(tmpdir, network_id)
    with open(net_path, 'wb') as f:
        resp = source_client.get_network_as_cx_stream(network_id)
        for chunk in resp.iter_content(chunk_size=8192):
            if chunk:
                f.write(chunk)
    return net_path


def upload_network(dest_client=None, net_path=None):
    """

    :param dest_client:
    :param net_path:
    :return:
    """
    with open(net_path, 'rb') as f:
        res = dest_client.save_cx_stream_as_new_network(f, visibility='PUBLIC')
        return res[res.rindex('/')+1:]


def add_network_to_networkset(dest_client=None,
                              netset_id=None, network_id=None):
    """

    :param dest_client:
    :param netset_id:
    :param network_id:
    :return:
    """
    dest_client.add_networks_to_networkset(netset_id, [network_id])


def copy_database(tempdir=None, source_client=None, dest_client=None, entry=None):
    """

    :param source_client:
    :param dest_client:
    :param entry:
    :return:
    """
    LOGGER.info('Creating networkset for ' + entry['name'])
    netset_raw = dest_client.create_networkset('Pathway relevance ' +
                                               entry['name'] + ' database',
                                               'Networkset to hold ' +
                                               entry['name'] +
                                               ' networks for Pathway relevance REST service')
    netset_id = netset_raw[netset_raw.rindex('/')+1:]
    counter = 0
    num_networks = len(entry['networks'])
    for network_id in entry['networks']:
        LOGGER.debug('Downloading ' + network_id + ' network which is ' +
                     str(counter) + ' of ' + str(num_networks) +
                     ' for database ' + entry['name'])
        net_path = download_network(tmpdir=tempdir,
                                    source_client=source_client,
                                    network_id=network_id)
        network_id = upload_network(dest_client=dest_client, net_path=net_path)
        os.unlink(net_path)
        add_network_to_networkset(dest_client=dest_client,
                                  netset_id=netset_id, network_id=network_id)
        counter += 1
    return netset_id


def copy_networks(tempdir=None,
                  source_client=None,
                  dest_client=None, dbresults=None):
    """

    :param source_client:
    :param dest_client:
    :param dbresults:
    :return:
    """
    temp_dir = tempfile.mkdtemp(dir=tempdir)
    LOGGER.info('Creating temp directory: ' + temp_dir +
                 ' to temporarily hold CX files')
    try:
        for entry in dbresults['results']:
            networkset = copy_database(tempdir=temp_dir,
                                       source_client=source_client,
                                       dest_client=dest_client, entry=entry)
            entry['networkSetId'] = networkset
    finally:
        shutil.rmtree(temp_dir)


def cleanup_dbresults(dbresults, dest_server=None,
                      dest_user=None, dest_pass=None):
    """

    :param dbresults:
    :return:
    """
    new_networkset_map = {}
    for entry in dbresults['results']:
        new_networkset_map[entry['uuid']] = entry['networkSetId']
        entry['networks'] = []
        del entry['networkSetId']

    for ckey in dbresults['databaseConnectionMap'].keys():
        if ckey not in new_networkset_map:
            LOGGER.error('Not sure how but ' + ckey +
                         ' is not in databaseConnectionMap. Skipping...')
            continue
        con_entry = dbresults['databaseConnectionMap'][ckey]
        con_entry['networkSetId'] = new_networkset_map[ckey]
        con_entry['server'] = dest_server
        con_entry['user'] = dest_user
        con_entry['password'] = dest_pass


def copy_data(theargs):
    """

    :param theargs:
    :return:
    """
    theargs.source_client = get_client(theargs.source_server,
                                       theargs.source_user,
                                       theargs.source_pass)
    theargs.dest_client = get_client(theargs.dest_server,
                                     theargs.dest_user,
                                     theargs.dest_pass)

    dbresults = load_dbresults_and_populate_networkids(theargs.inputdbresults)
    update_dbresults_with_networks_for_each_result(source_client=theargs.source_client,
                                                   dbresults=dbresults)
    copy_networks(source_client=theargs.source_client,
                  dest_client=theargs.dest_client, dbresults=dbresults)

    cleanup_dbresults(dbresults, dest_server=theargs.dest_server,
                      dest_user=theargs.dest_user, dest_pass=theargs.dest_pass)

    write_output_dbresults(theargs, dbresults)
    sys.stdout.write('\nProcessing complete. Have a nice day.\n\n')
    return 0


def main(arglist):
    desc = """
              This program takes an input dbresults.json/databaseresults.json
              and copies all networks found on a source NDEx server to a
              another destination NDEx server. A new dbresults.json file 
              is generated which can be used as input
              for pathway relevance/enrichment REST service.
              
              To use an account on the source NDEx server must exist which 
              has access to all networks in input dbresults.json file and
              an NDEx account must exist on destination NDEx server where
              all networks and networksets will be stored.
              
              This program will prompt the user for credentials for source
              and destination servers.
    """
    theargs = _parse_arguments(desc, arglist[1:])
    _setup_logging(theargs)
    theargs.source_server = input('Enter source NDEx server '
                          '(default public.ndexbio.org): ')
    if theargs.source_server is None or len(theargs.source_server) == 0:
        theargs.source_server = 'public.ndexbio.org'

    theargs.source_user = input('Enter source NDEx user: ')
    theargs.source_pass = getpass.getpass(prompt='Enter source '
                                                 'NDEx password: ')

    theargs.dest_server = input('Enter destination NDEx server '
                                '(default localhost): ')
    if len(theargs.dest_server) == 0:
        theargs.dest_server = 'localhost'

    theargs.dest_user = input('Enter destination NDEx user: ')
    theargs.dest_pass = getpass.getpass(prompt='Enter destination '
                                               'NDEx password: ')

    sys.stdout.write('\n\tSource NDEx Server: (' + theargs.source_server +
                     ') connecting with user: ' + theargs.source_user + '\n\n')
    sys.stdout.write('\tDestination NDEx Server: ' + theargs.dest_server +
                     ' connecting user: ' + theargs.dest_user + '\n\n')

    is_right = input('Is this correct? (y|n): ')
    if is_right != 'y':
        sys.stdout.write('\n\t' + str(is_right) + ' entered so exiting now. '
                                                  'Have a nice day\n\n')
        return 2

    return copy_data(theargs)



if __name__ == '__main__':  # pragma: no cover
    sys.exit(main(sys.argv))


