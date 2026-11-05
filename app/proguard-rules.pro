# Служба вызывается системой по имени из манифеста, R8 сам сохранит её через манифест.
# Метод скрытого API вызывается через рефлексию — его нужно оставить:
-keep class android.app.StatusBarManager { *; }
-dontwarn android.app.StatusBarManager
