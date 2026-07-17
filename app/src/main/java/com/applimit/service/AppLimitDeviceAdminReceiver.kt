package com.applimit.service

import android.app.admin.DeviceAdminReceiver

/**
 * Device-admin receiver. Being an active device admin unlocks
 * [android.app.admin.DevicePolicyManager.lockNow], which is what the full lock
 * uses to force the phone to its lockscreen.
 *
 * NOTE ON LOCK STRENGTH — see DeviceLockController for the full explanation:
 *  - As a plain device admin (what a normal user can enable in Settings) we get
 *    lockNow() only. That pushes the device to the secure lockscreen but does
 *    NOT disable the power button or emergency calling.
 *  - The uncircumventable, Family-Link-grade lock requires DEVICE OWNER, which
 *    can only be provisioned on a freshly factory-reset device (QR/NFC/afw).
 */
class AppLimitDeviceAdminReceiver : DeviceAdminReceiver()
