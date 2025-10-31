package com.nutech.ppob.dto.content;

public record ServiceItem(
  String service_code,
  String service_name,
  String service_icon,
  long   service_tariff
) {}
