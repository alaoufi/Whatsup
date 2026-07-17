#!/usr/bin/env python3
# مولّد أكواد التفعيل (Keygen) لتطبيق WhatsAppReminder
#
# ⚠️ سرّي: هذا الملف يحتوي السرّ الذي يُولّد الأكواد — لا تشاركه.
# يجب أن تطابق القيم أدناه نفس القيم في LicenseManager.kt.
#
# الاستخدام:
#   python3 keygen.py                → يسأل عن رمز الجهاز
#   python3 keygen.py A1B2-C3D4-E5F6 → يطبع كود التفعيل مباشرة

import hashlib
import sys

# نفس قيم LicenseManager.kt
SECRET = "WAR-2026-#Alaoufi#-KEY"
UNIVERSAL_CODE = "UNIV1-WAR2026"


def sha256(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def fmt(hex_str: str) -> str:
    up = hex_str.upper()
    return f"{up[0:4]}-{up[4:8]}-{up[8:12]}"


def activation_code(device_code: str) -> str:
    device_code = device_code.strip().upper()
    return fmt(sha256(SECRET + device_code))


if __name__ == "__main__":
    if len(sys.argv) > 1:
        device = sys.argv[1]
    else:
        device = input("أدخل رمز الجهاز (Device code): ")

    print()
    print("الكود العالمي (يعمل على أي جهاز):", UNIVERSAL_CODE)
    print("كود التفعيل لهذا الجهاز        :", activation_code(device))
