package com.example.myapplication.utils

import com.example.myapplication.models.PhoneDevice
import com.example.myapplication.models.SaleRecord

object InvoiceGenerator {

    private const val SHOP_NAME = "PRO TELEcom"
    private const val SHOP_PHONE = "0663022418"

    fun generateDeviceReceipt(device: PhoneDevice): String {
        val date = PlatformUtils.formatDate(device.entryDate)
        return """
            <div dir="rtl" style="font-family: 'Arial', sans-serif; padding: 20px; border: 2px solid #333; max-width: 400px; margin: auto;">
                <div style="text-align: center; border-bottom: 2px solid #333; padding-bottom: 10px; margin-bottom: 20px;">
                    <h1 style="margin: 0;">$SHOP_NAME</h1>
                    <p style="margin: 5px 0;">بيع وصيانة الهواتف</p>
                    <p style="margin: 5px 0;">الهاتف: $SHOP_PHONE</p>
                </div>
                
                <div style="margin-bottom: 20px;">
                    <p><b>رقم التذكرة:</b> ${device.deviceId}</p>
                    <p><b>التاريخ:</b> $date</p>
                    <p><b>الزبون:</b> ${device.customerName}</p>
                </div>
                
                <table style="width: 100%; border-collapse: collapse; margin-bottom: 20px;">
                    <tr style="background: #eee;">
                        <th style="padding: 10px; border: 1px solid #333; text-align: right;">الجهاز</th>
                        <th style="padding: 10px; border: 1px solid #333; text-align: right;">العطل</th>
                    </tr>
                    <tr>
                        <td style="padding: 10px; border: 1px solid #333;">${device.modelName}</td>
                        <td style="padding: 10px; border: 1px solid #333;">${device.issueDescription}</td>
                    </tr>
                </table>
                
                <div style="border: 1px solid #333; padding: 10px; margin-bottom: 20px;">
                    <p style="margin: 0; font-weight: bold;">التكلفة المقدرة: ${device.estimatedCost.toInt()} DA</p>
                </div>
                
                <div style="font-size: 11px; border-top: 1px dashed #333; padding-top: 10px;">
                    <p style="font-weight: bold; margin-bottom: 5px;">ملاحظات هامة:</p>
                    <p style="margin: 3px 0;">* يرجى إحضار هذا الوصل عند استلام الجهاز.</p>
                    <p style="margin: 3px 0;">* المحل غير مسؤول عن الأجهزة التي مر عليها أكثر من 3 أشهر.</p>
                </div>
                
                <div style="text-align: center; margin-top: 20px;">
                    <p style="font-weight: bold;">شكراً لثقتكم بنا!</p>
                </div>
            </div>
        """.trimIndent()
    }

    fun generateSaleInvoice(sale: SaleRecord): String {
        val date = PlatformUtils.formatDate(sale.date)
        return """
            <div dir="rtl" style="font-family: 'Arial', sans-serif; padding: 20px; border: 2px solid #333; max-width: 400px; margin: auto;">
                <div style="text-align: center; border-bottom: 2px solid #333; padding-bottom: 10px; margin-bottom: 20px;">
                    <h1 style="margin: 0;">$SHOP_NAME</h1>
                    <p style="margin: 5px 0;">بيع وصيانة الهواتف</p>
                    <p style="margin: 5px 0;">الهاتف: $SHOP_PHONE</p>
                </div>
                
                <div style="font-size: 14px; margin-bottom: 20px;">
                    <p><b>رقم الفاتورة:</b> ${sale.saleId}</p>
                    <p><b>التاريخ:</b> $date</p>
                    <p><b>البائع:</b> ${sale.processedBy}</p>
                </div>
                
                <table style="width: 100%; border-collapse: collapse; margin-bottom: 20px;">
                    <tr style="background: #eee;">
                        <th style="padding: 10px; border: 1px solid #333; text-align: right;">البيان</th>
                        <th style="padding: 10px; border: 1px solid #333; text-align: right;">المبلغ</th>
                    </tr>
                    <tr>
                        <td style="padding: 10px; border: 1px solid #333;">${sale.description}</td>
                        <td style="padding: 10px; border: 1px solid #333;">${sale.amount.toInt()} DA</td>
                    </tr>
                </table>
                
                <div style="text-align: left; font-size: 18px; font-weight: bold; margin-bottom: 20px;">
                    <p>الإجمالي: ${sale.amount.toInt()} DA</p>
                </div>
                
                <div style="font-size: 11px; border-top: 1px dashed #333; padding-top: 10px;">
                    <p style="font-weight: bold; margin-bottom: 5px;">ملاحظات هامة:</p>
                    <p style="margin: 3px 0;">- الضمان لمدة 15 يوم من تاريخ البيع.</p>
                    <p style="margin: 3px 0;">- لا يشمل الضمان الكسر أو سوء الاستخدام.</p>
                    <p style="margin: 3px 0;">- يرجى الاحتفاظ بهذه الفاتورة.</p>
                </div>
                
                <div style="text-align: center; margin-top: 30px;">
                    <p style="font-weight: bold;">شكراً لثقتكم بنا!</p>
                </div>
            </div>
        """.trimIndent()
    }
}
