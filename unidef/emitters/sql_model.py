from unidef.emitters import Emitter
from unidef.languages.common.type_model import DyType, FieldType, Traits
from unidef.models.config_model import ModelDefinition
from unidef.utils.name_convert import *


def get_real(ty: DyType) -> str:
    assert ty.get_field(Traits.Floating), True
    bits = ty.get_field(Traits.BitSize)
    if bits == 32:
        return "float"
    elif bits == 64:
        return "double precision"
    else:
        raise NotImplementedError()


def get_integer(ty: DyType) -> str:
    assert ty.get_field(Traits.Integer), True
    bits = ty.get_field(Traits.BitSize)

    if bits < 32:
        return "smallint"
    elif bits == 32:
        return "int"
    elif bits > 32:
        return "bigint"
    else:
        raise NotImplementedError()


def map_type_to_ddl(ty: DyType) -> str:
    assert ty is not None
    if ty.get_field(Traits.Floating):
        return get_real(ty)

    if ty.get_field(Traits.TsUnit):
        return "timestamp without time zone"

    if ty.get_field(Traits.Integer):
        return get_integer(ty)

    if ty.get_field(Traits.String) or ty.get_field(Traits.Null):
        return "text"

    if ty.get_field(Traits.Bool):
        return "bool"

    if ty.get_field(Traits.Struct):
        return "jsonb"

    if ty.get_field(Traits.Enum):
        if ty.get_field(Traits.SimpleEnum):
            return "text"
        else:
            return "jsonb"
    raise Exception("Cannot map {} to sql type".format(ty.get_field(Traits.TypeName)))


def get_field(field: FieldType) -> str:
    base = to_snake_case(field.field_name) + " " + map_type_to_ddl(field.field_type)
    if field.get_field(Traits.Primary):
        base += " primary key"

    if not field.get_field(Traits.Nullable):
        base += " not null"

    return base


def emit_schema_from_model(model: DyType) -> str:
    fields = ",\n".join(
        [get_field(field) for field in model.get_field(Traits.StructFields)]
    )
    return fields


def emit_field_names_from_model(model: DyType) -> str:
    fields = ",".join(
        [
            field.get_field(Traits.TypeName)
            for field in model.get_field(Traits.StructFields)
        ]
    )
    return fields


class SqlEmitter(Emitter):
    def accept(self, s: str) -> bool:
        return s == "sql"

    def emit_model(self, target: str, model: ModelDefinition) -> str:
        return emit_schema_from_model(model.get_parsed())

    def emit_type(self, target: str, ty: DyType) -> str:
        return emit_schema_from_model(ty)
