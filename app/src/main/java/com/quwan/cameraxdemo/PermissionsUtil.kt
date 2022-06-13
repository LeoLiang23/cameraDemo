package com.quwan.cameraxdemo

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.util.ArrayList

/**
 * Time:2021/11/30 16:15
 * Author: Liang Jing Jie
 * Description:
 */
object PermissionsUtil {

    private const val TAG = "PermissionsUtil"
    private const val PERMISSION_REQUESTS = 1

    fun requestPermission(context: Context) {
        if (!allPermissionsGranted(context)) {
            runtimePermissions(context)
        }
    }

    private fun isPermissionGranted(context: Context, permission: String?): Boolean {
        if (ContextCompat.checkSelfPermission(context, permission!!) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    fun allPermissionsGranted(context: Context): Boolean {
        for (permission in requiredPermissions(context)) {
            if (!isPermissionGranted(context, permission)) {
                return false
            }
        }
        return true
    }

    private fun requiredPermissions(context: Context): Array<String?> {
        val info =
            context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val ps = info.requestedPermissions
        if (ps != null && ps.isNotEmpty()) {
            ps
        } else {
            arrayOfNulls(0)
        }
        return ps
    }

    private fun runtimePermissions(context: Context) {
        val allNeededPermissions: MutableList<String?> = ArrayList()
        for (permission in requiredPermissions(context)) {
            if (!isPermissionGranted(context, permission)) {
                allNeededPermissions.add(permission)
            }
        }
        if (allNeededPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                getActivity(context),
                allNeededPermissions.toTypedArray(),
                PERMISSION_REQUESTS
            )
        }
    }

    @SuppressLint("UseRequireInsteadOfGet")
    private fun getActivity(context: Context) = when (context) {
        is Fragment -> {
            (context as Fragment).activity!!
        }
        is Activity -> {
            context
        }
        else -> {
            throw RuntimeException("context 必须是 Activity 或者 Fragment ")
        }
    }
}