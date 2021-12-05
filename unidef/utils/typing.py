import logging
import sys
from pydantic import BaseModel, validator, root_validator
from enum import Enum
from beartype import beartype
from abc import *
from typing import (
    Any,
    Callable,
    Dict,
    Iterable,
    Iterator,
    List,
    Optional,
    Set,
    Tuple,
    Union,
    Generic,
    TypeVar,
)

# handle type annotation changes in PEP 3107, 484, 526, 544, 560, 563, 585
if sys.version_info >= (3, 8, 8):
    Dict = dict
    List = list
    Set = set
    Tuple = tuple
    import collections.abc

    Iterable = collections.abc.Iterable
    Iterator = collections.abc.Iterator
    if sys.version_info >= (3, 9, 2):
        Callable = collections.abc.Callable
    else:
        Callable = collections.abc.Callable
        logging.warning("collections.abc.Callable in Python 3.9.1 is broken")

def abstract(cls):
    old_init = cls.__init__

    def new_init(self, *args, **kwargs):
        if type(self) == cls:
            raise TypeError(f"{cls} is abstract and cannot be initialized here")
        old_init(self, *args, **kwargs)

    cls.__init__ = new_init
    return cls