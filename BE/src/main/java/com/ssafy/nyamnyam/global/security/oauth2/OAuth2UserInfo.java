package com.ssafy.nyamnyam.global.security.oauth2;

import lombok.Getter;

import java.util.Map;

@Getter
public class OAuth2UserInfo {

    private final String providerUserId;
    private final String email;
    private final String name;
    private final String picture;

    public OAuth2UserInfo(Map<String, Object> attributes) {
        this.providerUserId = (String) attributes.get("sub");
        this.email = (String) attributes.get("email");
        this.name = (String) attributes.get("name");
        this.picture = (String) attributes.get("picture");
    }
}
