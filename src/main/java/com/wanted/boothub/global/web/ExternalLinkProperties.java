package com.wanted.boothub.global.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 헤더에 고정으로 노출되는 외부 링크 (application.yaml 의 boothub.links.*).
 */
@ConfigurationProperties(prefix = "boothub.links")
public record ExternalLinkProperties(String slack, String github, String notion) {
}
