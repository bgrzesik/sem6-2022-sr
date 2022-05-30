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


DESCRIPTOR = _descriptor_pool.Default().AddSerializedFile(b'\n\nchat.proto\x1a\x1bgoogle/protobuf/empty.proto\"\x15\n\x05Group\x12\x0c\n\x04name\x18\x01 \x01(\t\"/\n\x0e\x46ileAttachment\x12\x0c\n\x04mime\x18\x01 \x01(\t\x12\x0f\n\x07\x63ontent\x18\x02 \x01(\x0c\"\xba\x01\n\x0b\x43hatMessage\x12\x11\n\tmessageId\x18\x01 \x01(\t\x12\x15\n\x05group\x18\x02 \x01(\x0b\x32\x06.Group\x12\x0e\n\x06\x61uthor\x18\x03 \x01(\t\x12\x0f\n\x07\x63ontent\x18\x04 \x01(\t\x12\x10\n\x08priority\x18\x07 \x01(\x05\x12\x1f\n\trepliesTo\x18\x08 \x01(\x0b\x32\x0c.ChatMessage\x12\x1f\n\x04\x66ile\x18\x64 \x01(\x0b\x32\x0f.FileAttachmentH\x00\x42\x0c\n\nattachment\"E\n\x17GroupManagerGetMessages\x12\x15\n\x05group\x18\x01 \x01(\x0b\x32\x06.Group\x12\x13\n\x0brecipientId\x18\x02 \x01(\t\"\'\n\x17GroupRepositoryGetGroup\x12\x0c\n\x04name\x18\x01 \x01(\t\"(\n\x18GroupRepositoryGetGroups\x12\x0c\n\x04name\x18\x01 \x03(\t2v\n\x0cGroupManager\x12+\n\x0bSendMessage\x12\x0c.ChatMessage\x1a\x0c.ChatMessage\"\x00\x12\x39\n\x0bGetMessages\x12\x18.GroupManagerGetMessages\x1a\x0c.ChatMessage\"\x00\x30\x01\x32\x83\x01\n\x0fGroupRepository\x12.\n\x08GetGroup\x12\x18.GroupRepositoryGetGroup\x1a\x06.Group\"\x00\x12@\n\tGetGroups\x12\x16.google.protobuf.Empty\x1a\x19.GroupRepositoryGetGroups\"\x00\x42%\n pl.edu.agh.student.sr.lab5.proto\x88\x01\x01\x62\x06proto3')



_GROUP = DESCRIPTOR.message_types_by_name['Group']
_FILEATTACHMENT = DESCRIPTOR.message_types_by_name['FileAttachment']
_CHATMESSAGE = DESCRIPTOR.message_types_by_name['ChatMessage']
_GROUPMANAGERGETMESSAGES = DESCRIPTOR.message_types_by_name['GroupManagerGetMessages']
_GROUPREPOSITORYGETGROUP = DESCRIPTOR.message_types_by_name['GroupRepositoryGetGroup']
_GROUPREPOSITORYGETGROUPS = DESCRIPTOR.message_types_by_name['GroupRepositoryGetGroups']
Group = _reflection.GeneratedProtocolMessageType('Group', (_message.Message,), {
  'DESCRIPTOR' : _GROUP,
  '__module__' : 'chat_pb2'
  # @@protoc_insertion_point(class_scope:Group)
  })
_sym_db.RegisterMessage(Group)

FileAttachment = _reflection.GeneratedProtocolMessageType('FileAttachment', (_message.Message,), {
  'DESCRIPTOR' : _FILEATTACHMENT,
  '__module__' : 'chat_pb2'
  # @@protoc_insertion_point(class_scope:FileAttachment)
  })
_sym_db.RegisterMessage(FileAttachment)

ChatMessage = _reflection.GeneratedProtocolMessageType('ChatMessage', (_message.Message,), {
  'DESCRIPTOR' : _CHATMESSAGE,
  '__module__' : 'chat_pb2'
  # @@protoc_insertion_point(class_scope:ChatMessage)
  })
_sym_db.RegisterMessage(ChatMessage)

GroupManagerGetMessages = _reflection.GeneratedProtocolMessageType('GroupManagerGetMessages', (_message.Message,), {
  'DESCRIPTOR' : _GROUPMANAGERGETMESSAGES,
  '__module__' : 'chat_pb2'
  # @@protoc_insertion_point(class_scope:GroupManagerGetMessages)
  })
_sym_db.RegisterMessage(GroupManagerGetMessages)

GroupRepositoryGetGroup = _reflection.GeneratedProtocolMessageType('GroupRepositoryGetGroup', (_message.Message,), {
  'DESCRIPTOR' : _GROUPREPOSITORYGETGROUP,
  '__module__' : 'chat_pb2'
  # @@protoc_insertion_point(class_scope:GroupRepositoryGetGroup)
  })
_sym_db.RegisterMessage(GroupRepositoryGetGroup)

GroupRepositoryGetGroups = _reflection.GeneratedProtocolMessageType('GroupRepositoryGetGroups', (_message.Message,), {
  'DESCRIPTOR' : _GROUPREPOSITORYGETGROUPS,
  '__module__' : 'chat_pb2'
  # @@protoc_insertion_point(class_scope:GroupRepositoryGetGroups)
  })
_sym_db.RegisterMessage(GroupRepositoryGetGroups)

_GROUPMANAGER = DESCRIPTOR.services_by_name['GroupManager']
_GROUPREPOSITORY = DESCRIPTOR.services_by_name['GroupRepository']
if _descriptor._USE_C_DESCRIPTORS == False:

  DESCRIPTOR._options = None
  DESCRIPTOR._serialized_options = b'\n pl.edu.agh.student.sr.lab5.proto\210\001\001'
  _GROUP._serialized_start=43
  _GROUP._serialized_end=64
  _FILEATTACHMENT._serialized_start=66
  _FILEATTACHMENT._serialized_end=113
  _CHATMESSAGE._serialized_start=116
  _CHATMESSAGE._serialized_end=302
  _GROUPMANAGERGETMESSAGES._serialized_start=304
  _GROUPMANAGERGETMESSAGES._serialized_end=373
  _GROUPREPOSITORYGETGROUP._serialized_start=375
  _GROUPREPOSITORYGETGROUP._serialized_end=414
  _GROUPREPOSITORYGETGROUPS._serialized_start=416
  _GROUPREPOSITORYGETGROUPS._serialized_end=456
  _GROUPMANAGER._serialized_start=458
  _GROUPMANAGER._serialized_end=576
  _GROUPREPOSITORY._serialized_start=579
  _GROUPREPOSITORY._serialized_end=710
# @@protoc_insertion_point(module_scope)
