package com.handydev.main.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.fragment.app.ListFragment
import com.handydev.financisto.R
import com.handydev.financisto.activity.Report2DChartActivity
import com.handydev.financisto.activity.ReportActivity
import com.handydev.financisto.adapter.SummaryEntityListAdapter
import com.handydev.financisto.db.MyEntityManager
import com.handydev.financisto.graph.Report2DChart
import com.handydev.financisto.report.Report
import com.handydev.financisto.report.ReportType
import com.handydev.financisto.utils.MyPreferences
import com.handydev.financisto.utils.PinProtection
import java.util.*

class ReportsListFragment: ListFragment() {
    companion object {
        @JvmStatic fun createReport(context: Context?, em: MyEntityManager, extras: Bundle): Report? {
            val reportTypeName = extras.getString(EXTRA_REPORT_TYPE)
            val reportType = ReportType.valueOf(reportTypeName!!)
            val c = em.homeCurrency
            return reportType.createReport(context, c)
        }
        val EXTRA_REPORT_TYPE = "reportType"
    }

    private lateinit var reports: Array<ReportType>

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        reports = getReportsList()
        listAdapter = SummaryEntityListAdapter(activity!!, reports)
        return inflater.inflate(R.layout.reports_list, container, false)
    }

    override fun onPause() {
        super.onPause()
        if(activity != null) {
            PinProtection.lock(activity!!)
        }
    }

    override fun onResume() {
        super.onResume()
        if(activity != null) {
            PinProtection.unlock(activity!!)
        }
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        super.onListItemClick(l, v, position, id)
        if(activity == null) {
            return
        }
        if (reports[position].isConventionalBarReport) {
            // Conventional Bars reports
            val intent = Intent(activity!!, ReportActivity::class.java)
            intent.putExtra(EXTRA_REPORT_TYPE, reports[position].name)
            startActivity(intent)
        } else {
            // 2D Chart reports
            val intent = Intent(activity!!, Report2DChartActivity::class.java)
            intent.putExtra(Report2DChart.REPORT_TYPE, reports[position].name)
            startActivity(intent)
        }
    }

    private fun getReportsList(): Array<ReportType> {
        val reports = ArrayList<ReportType>()
        if(activity != null) {
            reports.add(ReportType.BY_PERIOD)
            reports.add(ReportType.BY_CATEGORY)
            if (MyPreferences.isShowPayee(activity!!)) {
                reports.add(ReportType.BY_PAYEE)
            }
            if (MyPreferences.isShowLocation(activity!!)) {
                reports.add(ReportType.BY_LOCATION)
            }
            if (MyPreferences.isShowProject(activity!!)) {
                reports.add(ReportType.BY_PROJECT)
            }
            reports.add(ReportType.BY_ACCOUNT_BY_PERIOD)
            reports.add(ReportType.BY_CATEGORY_BY_PERIOD)
            if (MyPreferences.isShowPayee(activity!!)) {
                reports.add(ReportType.BY_PAYEE_BY_PERIOD)
            }
            if (MyPreferences.isShowLocation(activity!!)) {
                reports.add(ReportType.BY_LOCATION_BY_PERIOD)
            }
            if (MyPreferences.isShowProject(activity!!)) {
                reports.add(ReportType.BY_PROJECT_BY_PERIOD)
            }
        }
        return reports.toTypedArray()
    }
}