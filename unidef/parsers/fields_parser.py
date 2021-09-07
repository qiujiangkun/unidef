import re
import unicodedata

import pyhocon

from unidef.models.input_model import InputDefinition, FieldsInput
from unidef.models.type_model import *
from unidef.parsers import Parser
from unidef.utils.typing_compat import *


class FieldsParser(Parser):
    def accept(self, fmt: InputDefinition) -> bool:
        return isinstance(fmt, FieldsInput)

    def parse_field(self, field: Dict[str, Any]) -> Type:
        field = field.copy()
        name = field.pop("name")
        type_ref = field.pop("type")
        ty = GLOBAL_TYPE_REGISTRY.get_type(type_ref)
        if ty:
            ty = ty.copy()
            ty.replace_field(Traits.TypeName(name))
        else:
            ty = Type.from_str(name).append_field(Traits.TypeRef(type_ref))

        for key, val in field.items():
            trait = GLOBAL_TYPE_REGISTRY.get_field(key)
            if trait is not None:
                ty.append_field(trait.default_present(val))
            else:
                raise Exception("InvalidArgumentException: " + key)
        return ty

    def parse(self, name: str, fmt: InputDefinition) -> Type:
        assert isinstance(fmt, FieldsInput)
        fields = []
        for field in fmt.__root__:
            fields.append(self.parse_field(field))

        return Types.struct(name, fields)
