package wcg.yhm.ymmknowledgeexchange.common.utils;

import cn.hutool.core.util.StrUtil;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-28
 * @Description: 参数校验工具类
 * @Version: 1.0
 */

public class ParamUtils {

    /**
     * 昵称最小长度
     */
    private static final int MIN_NICK_NAME_LENGTH = 2;
    /**
     * 昵称最大长度
     */
    private static final int MAX_NICK_NAME_LENGTH = 24;
    /**
     * 密码最小长度
     */
    private static final int MIN_PASSWORD_LENGTH = 8;
    /**
     * 密码最大长度
     */
    private static final int MAX_PASSWORD_LENGTH = 16;
    /**
     * 密码组成规则(仅可由数字、字母和小数点组成)
     */
    private static final String RULES_PASSWORD = "^[a-zA-Z0-9.]+$\n";

    /**
     * 校验昵称长度是否合法
     * @param nickname 昵称
     * @return
     */
    public static boolean checkNickname(String nickname) {
        if (nickname.length() < MIN_NICK_NAME_LENGTH ||
                nickname.length() > MAX_NICK_NAME_LENGTH ) {
            return false;
        }
        return true;
    }

    /**
     * 检验密码是否合法
     * @param password
     * @return
     */
    public static boolean checkPassword(String password) {
        if (password.length() < MIN_PASSWORD_LENGTH ||
                password.length() > MAX_PASSWORD_LENGTH) {
            return false;
        }
        //密码是否符合规定
         return password.matches(RULES_PASSWORD);
    }

    /**
     * 检测文本长度
     * @param text
     * @param length
     * @return
     */
    public static boolean checkTextLength(String text, int length) {
        if (text.length() > length) {
            return false;
        }
        return true;
    }
}
    
