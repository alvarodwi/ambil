package me.varoa.ambil.model.exception

class SessionExpiredException :
    RuntimeException(
        "BlaBlaLink session expired. Restart ambil where a display is available and log in in the window that opens.",
    )
