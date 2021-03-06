# -*- coding: utf-8 -*-
# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: chat.proto
"""Generated protocol buffer code."""
from google.protobuf import descriptor as _descriptor
from google.protobuf import descriptor_pool as _descriptor_pool
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import symbol_database as _symbol_database
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()


from google.protobuf import empty_pb2 as google_dot_protobuf_dot_empty__pb2


DESCRIPTOR = _descriptor_pool.Default().AddSerializedFile(b'\n\nchat.proto\x12\x04\x63hat\x1a\x1bgoogle/protobuf/empty.proto\"\x15\n\x05Group\x12\x0c\n\x04name\x18\x01 \x01(\t\"/\n\x0e\x46ileAttachment\x12\x0c\n\x04mime\x18\x01 \x01(\t\x12\x0f\n\x07\x63ontent\x18\x02 \x01(\x0c\"\xc9\x01\n\x0b\x43hatMessage\x12\x11\n\tmessageId\x18\x01 \x01(\t\x12\x1a\n\x05group\x18\x02 \x01(\x0b\x32\x0b.chat.Group\x12\x0e\n\x06\x61uthor\x18\x03 \x01(\t\x12\x0f\n\x07\x63ontent\x18\x04 \x01(\t\x12\x10\n\x08priority\x18\x07 \x01(\x05\x12$\n\trepliesTo\x18\x08 \x01(\x0b\x32\x11.chat.ChatMessage\x12$\n\x04\x66ile\x18\x64 \x01(\x0b\x32\x14.chat.FileAttachmentH\x00\x42\x0c\n\nattachment\"J\n\x17GroupManagerGetMessages\x12\x1a\n\x05group\x18\x01 \x01(\x0b\x32\x0b.chat.Group\x12\x13\n\x0brecipientId\x18\x02 \x01(\t\"\'\n\x17GroupRepositoryGetGroup\x12\x0c\n\x04name\x18\x01 \x01(\t\"(\n\x18GroupRepositoryGetGroups\x12\x0c\n\x04name\x18\x01 \x03(\t2\x8a\x01\n\x0cGroupManager\x12\x35\n\x0bSendMessage\x12\x11.chat.ChatMessage\x1a\x11.chat.ChatMessage\"\x00\x12\x43\n\x0bGetMessages\x12\x1d.chat.GroupManagerGetMessages\x1a\x11.chat.ChatMessage\"\x00\x30\x01\x32\x92\x01\n\x0fGroupRepository\x12\x38\n\x08GetGroup\x12\x1d.chat.GroupRepositoryGetGroup\x1a\x0b.chat.Group\"\x00\x12\x45\n\tGetGroups\x12\x16.google.protobuf.Empty\x1a\x1e.chat.GroupRepositoryGetGroups\"\x00\x32\xbb\x01\n\x0eReflectionDemo\x12\x37\n\x05Test1\x12\x16.google.protobuf.Empty\x1a\x16.google.protobuf.Empty\x12\x37\n\x05Test2\x12\x16.google.protobuf.Empty\x1a\x16.google.protobuf.Empty\x12\x37\n\x05Test3\x12\x16.google.protobuf.Empty\x1a\x16.google.protobuf.EmptyB%\n pl.edu.agh.student.sr.lab5.proto\x88\x01\x01\x62\x06proto3')



_GROUP = DESCRIPTOR.message_types_by_name['Group']
_FILEATTACHMENT = DESCRIPTOR.message_types_by_name['FileAttachment']
_CHATMESSAGE = DESCRIPTOR.message_types_by_name['ChatMessage']
_GROUPMANAGERGETMESSAGES = DESCRIPTOR.message_types_by_name['GroupManagerGetMessages']
_GROUPREPOSITORYGETGROUP = DESCRIPTOR.message_types_by_name['GroupRepositoryGetGroup']
_GROUPREPOSITORYGETGROUPS = DESCRIPTOR.message_types_by_name['GroupRepositoryGetGroups']
Group = _reflection.GeneratedProtocolMessageType('Group', (_message.Message,), {
  'DESCRIPTOR' : _GROUP,
  '__module__' : 'chat_pb2'
  # @@protoc_insertion_point(class_scope:chat.Group)
  })
_sym_db.RegisterMessage(Group)

FileAttachment = _reflection.GeneratedProtocolMessageType('FileAttachment', (_message.Message,), {
  'DESCRIPTOR' : _FILEATTACHMENT,
  '__module__' : 'chat_pb2'
  # @@protoc_insertion_point(class_scope:chat.FileAttachment)
  })
_sym_db.RegisterMessage(FileAttachment)

ChatMessage = _reflection.GeneratedProtocolMessageType('ChatMessage', (_message.Message,), {
  'DESCRIPTOR' : _CHATMESSAGE,
  '__module__' : 'chat_pb2'
  # @@protoc_insertion_point(class_scope:chat.ChatMessage)
  })
_sym_db.RegisterMessage(ChatMessage)

GroupManagerGetMessages = _reflection.GeneratedProtocolMessageType('GroupManagerGetMessages', (_message.Message,), {
  'DESCRIPTOR' : _GROUPMANAGERGETMESSAGES,
  '__module__' : 'chat_pb2'
  # @@protoc_insertion_point(class_scope:chat.GroupManagerGetMessages)
  })
_sym_db.RegisterMessage(GroupManagerGetMessages)

GroupRepositoryGetGroup = _reflection.GeneratedProtocolMessageType('GroupRepositoryGetGroup', (_message.Message,), {
  'DESCRIPTOR' : _GROUPREPOSITORYGETGROUP,
  '__module__' : 'chat_pb2'
  # @@protoc_insertion_point(class_scope:chat.GroupRepositoryGetGroup)
  })
_sym_db.RegisterMessage(GroupRepositoryGetGroup)

GroupRepositoryGetGroups = _reflection.GeneratedProtocolMessageType('GroupRepositoryGetGroups', (_message.Message,), {
  'DESCRIPTOR' : _GROUPREPOSITORYGETGROUPS,
  '__module__' : 'chat_pb2'
  # @@protoc_insertion_point(class_scope:chat.GroupRepositoryGetGroups)
  })
_sym_db.RegisterMessage(GroupRepositoryGetGroups)

_GROUPMANAGER = DESCRIPTOR.services_by_name['GroupManager']
_GROUPREPOSITORY = DESCRIPTOR.services_by_name['GroupRepository']
_REFLECTIONDEMO = DESCRIPTOR.services_by_name['ReflectionDemo']
if _descriptor._USE_C_DESCRIPTORS == False:

  DESCRIPTOR._options = None
  DESCRIPTOR._serialized_options = b'\n pl.edu.agh.student.sr.lab5.proto\210\001\001'
  _GROUP._serialized_start=49
  _GROUP._serialized_end=70
  _FILEATTACHMENT._serialized_start=72
  _FILEATTACHMENT._serialized_end=119
  _CHATMESSAGE._serialized_start=122
  _CHATMESSAGE._serialized_end=323
  _GROUPMANAGERGETMESSAGES._serialized_start=325
  _GROUPMANAGERGETMESSAGES._serialized_end=399
  _GROUPREPOSITORYGETGROUP._serialized_start=401
  _GROUPREPOSITORYGETGROUP._serialized_end=440
  _GROUPREPOSITORYGETGROUPS._serialized_start=442
  _GROUPREPOSITORYGETGROUPS._serialized_end=482
  _GROUPMANAGER._serialized_start=485
  _GROUPMANAGER._serialized_end=623
  _GROUPREPOSITORY._serialized_start=626
  _GROUPREPOSITORY._serialized_end=772
  _REFLECTIONDEMO._serialized_start=775
  _REFLECTIONDEMO._serialized_end=962
# @@protoc_insertion_point(module_scope)
