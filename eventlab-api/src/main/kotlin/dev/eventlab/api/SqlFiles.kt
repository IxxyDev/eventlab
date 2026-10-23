package dev.eventlab.api

import org.springframework.core.io.ClassPathResource

fun loadSql(path: String): String =
  ClassPathResource("sql/$path").getContentAsString(Charsets.UTF_8)
