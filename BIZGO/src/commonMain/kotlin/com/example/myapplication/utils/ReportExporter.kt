package com.example.myapplication.utils

import com.example.myapplication.models.PhoneDevice
import com.example.myapplication.models.toArabic
import com.example.myapplication.viewmodel.ReportsData

object ReportExporter {

    fun generateCSV(data: ReportsData): String {
        val sb = StringBuilder()
        // Header مع دعم UTF-8 BOM للغة العربية في Excel
        sb.append("\uFEFF") 
        sb.append("معرف الجهاز,الموديل,الزبون,الحالة,التكلفة,التاريخ\n")

        data.devices.forEach { device ->
            sb.append("${device.deviceId},")
            sb.append("${device.modelName},")
            sb.append("${device.customerName},")
            sb.append("${device.status.toArabic()},")
            sb.append("${device.estimatedCost},")
            sb.append("${PlatformUtils.formatDate(device.entryDate)}\n")
        }

        sb.append("\n\n")
        sb.append("إحصائيات إجمالية\n")
        sb.append("إجمالي الإيرادات,${data.totalRevenue} DA\n")
        sb.append("صافي الأرباح,${data.netProfit} DA\n")
        sb.append("عدد الأجهزة المسجلة,${data.deviceCount}\n")
        
        return sb.toString()
    }

    fun generateHTML(data: ReportsData): String {
        return """
            <div dir="rtl" style="font-family: Arial, sans-serif; padding: 20px;">
                <h1 style="color: #6200EE; text-align: center;">تقرير الأداء المالي - BIZGO</h1>
                <hr>
                <div style="background: #f5f5f5; padding: 15px; border-radius: 10px; margin-bottom: 20px;">
                    <p><b>صافي الأرباح:</b> ${data.netProfit.toInt()} DA</p>
                    <p><b>إجمالي الإيرادات:</b> ${data.totalRevenue.toInt()} DA</p>
                    <p><b>عدد العمليات:</b> ${data.deviceCount}</p>
                </div>
                
                <table border="1" style="width: 100%; border-collapse: collapse; text-align: right;">
                    <tr style="background: #6200EE; color: white;">
                        <th style="padding: 10px;">الجهاز</th>
                        <th style="padding: 10px;">الزبون</th>
                        <th style="padding: 10px;">الحالة</th>
                        <th style="padding: 10px;">المبلغ</th>
                    </tr>
                    ${data.devices.joinToString("") { """
                        <tr>
                            <td style="padding: 8px;">${it.modelName}</td>
                            <td style="padding: 8px;">${it.customerName}</td>
                            <td style="padding: 8px;">${it.status.toArabic()}</td>
                            <td style="padding: 8px;">${it.estimatedCost.toInt()} DA</td>
                        </tr>
                    """ }}
                </table>
                <p style="text-align: center; color: gray; margin-top: 30px;">تم استخراج التقرير بتاريخ: ${PlatformUtils.formatDate(PlatformUtils.currentTimeMillis())}</p>
            </div>
        """.trimIndent()
    }
}
