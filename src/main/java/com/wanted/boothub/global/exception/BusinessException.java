package com.wanted.boothub.global.exception;

import lombok.Getter;

/**
 * 규칙 위반으로 요청을 처리할 수 없을 때 (중복 아이디, 비밀번호 규칙 위반 등).
 * field 가 있으면 컨트롤러가 그 입력칸의 오류로 화면에 보여준다.
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 오류를 붙일 폼 필드 이름. 특정 칸이 아니면 null */
    private final String field;

    public BusinessException(String field, String message) {
        super(message);
        this.field = field;
    }

    public BusinessException(String message) {
        this(null, message);
    }
}
