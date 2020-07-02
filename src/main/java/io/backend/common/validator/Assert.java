
package io.backend.common.validator;

import io.backend.common.exception.BackendException;
import org.apache.commons.lang.StringUtils;

/**
 * 数据校验
 *
 */
public abstract class Assert {

    public static void isBlank(String str, String message) {
        if (StringUtils.isBlank(str)) {
            throw new BackendException(message);
        }
    }

    public static void isNull(Object object, String message) {
        if (object == null) {
            throw new BackendException(message);
        }
    }
}
