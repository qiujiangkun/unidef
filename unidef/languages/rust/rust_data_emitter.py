import traceback

from unidef.emitters.sql_model import emit_schema_from_model
from unidef.languages.rust.rust_ast import *
from unidef.models.config_model import ModelDefinition
from unidef.utils.formatter import *
from unidef.utils.transformer import *


def find_all_structs_impl(reg: StructRegistry, s: DyType):
    if s.get_field(Traits.Struct):
        reg.add_struct(s)
        for field in s.get_field(Traits.StructFields):
            find_all_structs_impl(reg, field)
    for vt in s.get_field(Traits.ValueTypes):
        find_all_structs_impl(reg, vt)


def find_all_structs(s: DyType) -> List[DyType]:
    reg = StructRegistry()
    find_all_structs_impl(reg, s)
    return reg.structs


def sql_model_get_sql_ddl(struct: RustStructNode) -> RustFuncDeclNode:
    return RustFuncDeclNode(
        name="get_sql_ddl",
        args=[],
        ret=Types.String.copy()
            .append_field(Traits.Reference(True))
            .append_field(Traits.Lifetime("static")),
        content=f'r#"{emit_schema_from_model(struct.raw)}"#',
    )


def sql_model_get_value_inner(f: RustFieldNode) -> str:
    if f.value.get_field(Traits.Nullable):
        # TODO: nullable value not complete
        return 'self.{}.as_ref().map(|x| x.to_string()).unwrap_or("NULL".to_owned())'.format(
            f.name
        )
    elif f.value.get_field(Traits.Enum):
        if f.value.get_field(Traits.SimpleEnum):
            return "self.{}".format(f.name)
        else:
            return "serde_json::to_string(&self.{}).unwrap()".format(f.name)
    elif f.value.get_field(Traits.TsUnit):
        return "self.{}.val() as f64 * {}".format(
            f.name, to_second_scale(f.value.get_field(Traits.TsUnit))
        )
    elif (
            f.value.get_field(Traits.Struct)
            or f.value.get_field(Traits.Vector)
            or f.value.get_field(Traits.Tuple)
    ):
        return "serde_json::to_string(&self.{}).unwrap()".format(f.name)
    else:
        return "self.{}".format(f.name)


def sql_model_get_values_inner(struct: RustStructNode) -> List[str]:
    values = []
    for f in struct.fields:
        values.append(sql_model_get_value_inner(f))
    return values


def sql_model_field_names_in_format(struct: RustStructNode) -> str:
    fields = []
    for field in struct.fields:
        if field.value.get_field(Traits.SimpleEnum):
            fields.append("'{%s:?}'" % field.name)
        elif (
                field.value.get_field(Traits.String)
                or field.value.get_field(Traits.Enum)
                or field.value.get_field(Traits.Struct)
        ):
            fields.append("'{%s}'" % field.name)
        elif field.value.get_field(Traits.TsUnit):
            fields.append("to_timestamp({%s})" % field.name)
        else:
            fields.append("{%s}" % field.name)
    return ",".join(fields)


def sql_model_get_insert_into_sql(struct: RustStructNode) -> RustFuncDeclNode:
    values = sql_model_get_values_inner(struct)

    field_names = ",".join([field.name for field in struct.fields])
    field_names_in_format = sql_model_field_names_in_format(struct)
    arguments = ",\n".join(
        ["%s = %s" % (r_field.name, v) for (r_field, v) in zip(struct.fields, values)]
    )
    return RustFuncDeclNode(
        name="get_insert_into_sql",
        args=[
            RustArgumentPairNode(name="&self", type="Self"),
            RustArgumentPairNode(name="table", type="&str"),
        ],
        ret=Types.String,
        content=f"""format!(r#"INSERT INTO {{table}} ({field_names}) VALUES ({field_names_in_format});"#,
                     table=table, {arguments})""",
    )


def sql_model_get_field_sql(struct: RustStructNode) -> RustFuncDeclNode:
    field_names = ",".join([field.name for field in struct.fields])
    return RustFuncDeclNode(
        name="get_fields_sql",
        args=[RustArgumentPairNode(name="&self", type="Self")],
        ret=Types.String.copy()
            .append_field(Traits.Reference(True))
            .append_field(Traits.Lifetime("static")),
        content=f"""r#"{field_names}"#""",
    )


def sql_model_get_values_sql(struct: RustStructNode) -> RustFuncDeclNode:
    values = sql_model_get_values_inner(struct)

    field_names_in_format = sql_model_field_names_in_format(struct)
    lined = ',\n'.join(['%s = %s' % (r_field.name, v) for (r_field, v) in
                        zip(struct.fields, values)])
    return RustFuncDeclNode(
        name="get_values_sql",
        args=[RustArgumentPairNode(name="&self", type="Self")],
        ret=Types.String,
        content=f"""format!(r#"{field_names_in_format}"#, 
                   {lined})""",
    )


def sql_model_trait(struct: RustStructNode) -> RustAstNode:
    functions = [
        sql_model_get_sql_ddl(struct),
        sql_model_get_insert_into_sql(struct),
        sql_model_get_values_sql(struct),
        sql_model_get_field_sql(struct),
    ]

    return RustImplNode(name=struct.name, trait="SqlModel", functions=functions)


def from_sql_raw_func(struct: RustStructNode) -> RustFuncDeclNode:
    values = []
    for f in struct.fields:
        values.append(f"""row.get("{f.name}")""")

    content = f"""
     {struct.name} {{
        {','.join(['%s: %s' % (field.name, v) for (field, v) in zip(struct.fields, values)])}
     }}
    """
    return RustFuncDeclNode(
        name="from",
        args=[RustArgumentPairNode(name="row", type="Row")],
        ret=DyType.from_str("Self").append_field(Traits.TypeRef("Self")),
        content=content,
    )


def from_sql_raw_trait(struct: RustStructNode) -> Optional[RustAstNode]:
    for s in struct.fields:
        if (
                s.value.get_field(Traits.Enum)
                or s.value.get_field(Traits.Struct)
                or (
                s.value.get_field(Traits.Integer)
                and not s.value.get_field(Traits.Signed)
        )
                or s.value.get_field(Traits.TsUnit)
                or s.value.get_field(Traits.Vector)
        ):
            logging.warning(
                "Do not support %s %s yet, skipping From<Row>",
                s.name,
                s.value.get_field(Traits.TypeName),
            )
            return
    functions = [
        from_sql_raw_func(struct),
    ]

    return RustImplNode(name=struct.name, trait="From<Row>", functions=functions)


def raw_data_func(raw: str) -> RustFuncDeclNode:
    return RustFuncDeclNode(
        name="get_raw_data",
        args=[],
        ret=Types.String.copy()
            .append_field(Traits.Reference(True))
            .append_field(Traits.Lifetime("static")),
        content=f'r#"{raw}"#',
    )


@beartype
def emit_rust_type_inner(
        struct: DyType, root: Optional[ModelDefinition] = None
) -> Code:
    rust_formatter = RustFormatter()
    sources = []
    rust_struct = RustStructNode(struct)
    sources.append(rust_formatter.transform(rust_struct))
    if root and struct.get_field(Traits.TypeName) == root.name:
        funcs = [raw_data_func(root.raw)]
        sources.append(
            rust_formatter.transform(
                RustImplNode(name=rust_struct.name, functions=funcs)
            )
        )
    backup = sources[:]
    try:
        sources.append(rust_formatter.transform(sql_model_trait(rust_struct)))
        from_sql = from_sql_raw_trait(rust_struct)
        if from_sql:
            sources.append(rust_formatter.transform(from_sql))
    except Exception as e:
        logging.warning(
            "Error happened while generating sql_model_trait, skipping. %s", e
        )
        traceback.print_exc()
        sources = backup
    return Code("""{{ sources | join("") }}""", sources=sources)


def emit_rust_type(struct: DyType, root: Optional[ModelDefinition] = None) -> str:
    writer = StructuredFormatter(nodes=[emit_rust_type_inner(struct, root)])
    return writer.to_string()


def emit_rust_model_definition(root: ModelDefinition) -> str:
    rust_formatter = RustFormatter()
    formatter = StructuredFormatter()
    comment = []
    for attr in ["type", "url", "ref", "note"]:
        t = getattr(root, attr)
        if t:
            comment.extend(f"{attr}: {t}".splitlines())
    formatter.append_format_node(
        TextNode(str(rust_formatter.transform_rust_comment_node(RustCommentNode(comment, cargo_doc=True))))
    )
    parsed = root.get_parsed()
    if parsed.get_field(Traits.Struct):
        for struct in find_all_structs(parsed):
            if struct.get_field(Traits.TypeRef):
                continue
            formatter.append_format_node(TextNode(str(emit_rust_type_inner(struct, root))))

    elif parsed.get_field(Traits.Enum):
        rust_enum = RustEnumNode(parsed)
        formatter.append_format_node(TextNode(str(rust_formatter.transform(rust_enum))))
    else:
        raise Exception("must be a struct or enum", root)

    return try_rustfmt(formatter.to_string())
