// 비밀번호 로직
package physiosim.db;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public final class Passwords {
    private static final SecureRandom RNG = new SecureRandom();

    // 정책
    private static final int DEFAULT_ITER = 120_000;   // 권장 반복 횟수
    private static final int MIN_ACCEPTED_ITER = 50_000; // 너무 낮은 해시 거부
    private static final int SALT_LEN = 16;            // bytes
    private static final int KEY_LEN  = 256;           // bits
    private static final int MIN_PASSWORD_LEN = 8;     // 최소 비번 길이

    private Passwords() {}

    //비밀번호를 해시로 변환
    public static String hash(String plain) {
        // 입력 가드 + 최소 길이 체크
        if (plain == null || plain.isBlank())
            throw new IllegalArgumentException("Password must not be blank.");
        if (plain.length() < MIN_PASSWORD_LEN)
            throw new IllegalArgumentException("Password is too short (min " + MIN_PASSWORD_LEN + " chars).");

        byte[] salt = new byte[SALT_LEN];
        RNG.nextBytes(salt);

        byte[] dk = derive(plain.toCharArray(), salt, DEFAULT_ITER, KEY_LEN);
        return "pbkdf2$" + DEFAULT_ITER + "$" +
                Base64.getEncoder().encodeToString(salt) + "$" +
                Base64.getEncoder().encodeToString(dk);
    }

    // 해시 검증
    public static boolean verify(String plain, String stored) {
        try {
            // (검증 시에도 null/blank 방지)
            if (plain == null || plain.isBlank() || stored == null || stored.isBlank())
                return false;

            // 형식·정책 검증 강화
            String[] p = stored.split("\\$");
            if (p.length != 4) return false;
            if (!"pbkdf2".equals(p[0])) return false;

            int iter = Integer.parseInt(p[1]);
            if (iter < MIN_ACCEPTED_ITER) return false; // 너무 약한 해시X

            byte[] salt = Base64.getDecoder().decode(p[2]);
            byte[] expected = Base64.getDecoder().decode(p[3]);

            byte[] actual = derive(plain.toCharArray(), salt, iter, expected.length * 8);
            return constantTimeEq(expected, actual);
        } catch (Exception e) {
            return false;
        }
    }

    // 파생키
    private static byte[] derive(char[] pw, byte[] salt, int iter, int bits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(pw, salt, iter, bits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 상수시간 비교
    private static boolean constantTimeEq(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) return false;
        int r = 0;
        for (int i = 0; i < a.length; i++) r |= (a[i] ^ b[i]);
        return r == 0;
    }
}
