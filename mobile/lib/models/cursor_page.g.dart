// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'cursor_page.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

CursorPage<T> _$CursorPageFromJson<T>(
  Map<String, dynamic> json,
  T Function(Object? json) fromJsonT,
) => CursorPage<T>(
  content: (json['content'] as List<dynamic>).map(fromJsonT).toList(),
  hasNext: json['hasNext'] as bool,
  nextCursor: (json['nextCursor'] as num?)?.toInt(),
);

Map<String, dynamic> _$CursorPageToJson<T>(
  CursorPage<T> instance,
  Object? Function(T value) toJsonT,
) => <String, dynamic>{
  'content': instance.content.map(toJsonT).toList(),
  'hasNext': instance.hasNext,
  'nextCursor': instance.nextCursor,
};
