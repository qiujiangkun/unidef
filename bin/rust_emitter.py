#!/usr/bin/env python3

from unidef_emitter import *
import sys

if __name__ == '__main__':
    args = parser.parse_args()
    main('rust', args.format, open(args.file).read())