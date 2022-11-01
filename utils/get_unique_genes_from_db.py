#! /usr/bin/env python

import os
import sys
import getpass
import argparse
import json
import logging


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
    parser.add_argument('inputdbresults', help='Input databaseresults.json '
                                               'from '
                                               'pathway relevance or enrichment'
                                               'REST service')
    parser.add_argument('outfile', help='File to write gene list to, one per line')
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

    return parser.parse_args(args)


def get_unique_genes_from_db(inputfile):
    """

    :param source_client:
    :param inputfile:
    :return:
    """
    LOGGER.info('Loading input databaseresults')
    with open(inputfile, 'r') as f:
        dbresults = json.load(f)

    unique_genes = set()

    for gMap in dbresults['geneMapList']:
        unique_genes.update(gMap['geneMap'].keys())

    return list(unique_genes)


def main(arglist):
    desc = """
              This program takes an input databaseresults.json and
              outputs a list of unique genes found in the file

    """
    theargs = _parse_arguments(desc, arglist[1:])
    _setup_logging(theargs)

    unique_genes = get_unique_genes_from_db(theargs.inputdbresults)
    LOGGER.info(str(len(unique_genes)) + ' unique genes\n')
    with open(theargs.outfile, 'w') as f:
        for gene in unique_genes:
            f.write(gene + '\n')

    return 0


if __name__ == '__main__':  # pragma: no cover
    sys.exit(main(sys.argv))


