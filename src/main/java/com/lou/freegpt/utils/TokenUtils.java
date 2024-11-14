package com.lou.freegpt.utils;


import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;

import java.util.UUID;


public class TokenUtils {

//    public static void main(String[] args) {
//        String text = " ";
//        boolean blank = text.isBlank(); // 不见检查长度，还会检查空白、制表符等
//        boolean empty = text.isEmpty(); // 仅检查长度
//        System.out.println("blank: " + blank + ",empty: " + empty);
////        int tokenize = tokenize(text);
////        // 输出 token 数量
////        System.out.println("Token Count: " + tokenize);
//    }



    // Tokenize the input text and return the token count
    public static int tokenize(String text) {
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        Encoding enc = registry.getEncoding(EncodingType.CL100K_BASE);
        IntArrayList encoded = enc.encode(text);
        return encoded.size();
    }
}
