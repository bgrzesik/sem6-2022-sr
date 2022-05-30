import grpc

from grpc_reflection.v1alpha.proto_reflection_descriptor_database import ProtoReflectionDescriptorDatabase
from google.protobuf.descriptor import FieldDescriptor
from google.protobuf.descriptor_pool import DescriptorPool
from google.protobuf.message_factory import MessageFactory


def main():
    with grpc.insecure_channel('localhost:8080') as channel:
        db = ProtoReflectionDescriptorDatabase(channel)
        pool = DescriptorPool(db)

        services = db.get_services()

        if 'chat.ReflectionDemo' not in services:
            print('ReflectionDemo not present')
            return

        demo = pool.FindServiceByName('chat.ReflectionDemo')
        print(channel, dir(channel))
        methods = {}
        for i, method in enumerate(demo.methods_by_name):
            print(i, method)
            methods[i] = method

        i = int(input("Index>"))

        if i not in methods:
            print("Invalid index")
            return

        method = demo.FindMethodByName(methods[i])
        msg_factory = MessageFactory(pool)

        InputProto = msg_factory.GetPrototype(method.input_type)
        OutputProto = msg_factory.GetPrototype(method.output_type)
        method_call = channel.unary_unary(f'/chat.ReflectionDemo/{method.name}',
                                          InputProto.SerializeToString,
                                          OutputProto.FromString)

        input_proto = InputProto()

        for field in method.input_type.fields:
            if field.type == FieldDescriptor.CPPTYPE_DOUBLE:
                value = int(input(f'{field.name} = '))
                setattr(input_proto, field.name, value)

            elif field.type == FieldDescriptor.CPPTYPE_STRING:
                value = str(input(f'{field.name} = '))
                setattr(input_proto, field.name, value)

        output_proto = method_call(input_proto)
        print(output_proto)


if __name__ == '__main__':
    main()
