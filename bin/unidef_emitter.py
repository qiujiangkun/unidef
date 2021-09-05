#!/usr/bin/env python3
import logging
import os.path
import sys

logging.basicConfig(stream=sys.stderr)
sys.path.append(os.path.dirname(os.path.dirname(os.path.realpath(__file__))))

from unidef import parser, main, CommandLineConfig

if __name__ == '__main__':
    args = parser.parse_args()
    config = CommandLineConfig.from_args(args)
    main(config, open(args.file).read())
