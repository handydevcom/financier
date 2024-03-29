package com.handydev.financier.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.fragment.app.ListFragment
import com.handydev.financier.R
import com.handydev.financier.activity.Report2DChartActivity
import com.handydev.financier.activity.ReportActivity
import com.handydev.financier.adapter.SummaryEntityListAdapter
import com.handydev.financier.db.MyEntityManager
import com.handydev.financier.graph.Report2DChart
import com.handydev.financier.report.Report
import com.handydev.financier.report.ReportType
import com.handydev.financier.utils.MyPreferences
import com.handydev.financier.utils.PinProtection
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
        listAdapter = SummaryEntityListAdapter(requireActivity(), reports)
        return inflater.inflate(R.layout.reports_list, container, false)
    }

    override fun onPause() {
        super.onPause()
        if(activity != null) {
            PinProtection.lock(requireActivity())
        }
    }

    override fun onResume() {
        super.onResume()
        if(activity != null) {
            PinProtection.unlock(requireActivity())
        }
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        super.onListItemClick(l, v, position, id)
        if(activity == null) {
            return
        }
        if (reports[position].isConventionalBarReport) {
            // Conventional Bars reports
            val intent = Intent(requireActivity(), ReportActivity::class.java)
            intent.putExtra(EXTRA_REPORT_TYPE, reports[position].name)
            startActivity(intent)
        } else {
            // 2D Chart reports
            val intent = Intent(requireActivity(), Report2DChartActivity::class.java)
            intent.putExtra(Report2DChart.REPORT_TYPE, reports[position].name)
            startActivity(intent)
        }
    }

    private fun getReportsList(): Array<ReportType> {
        val reports = ArrayList<ReportType>()
        if(activity != null) {
            reports.add(ReportType.BY_PERIOD)
            reports.add(ReportType.BY_CATEGORY)
            if (MyPreferences.isShowPayee(requireActivity())) {
                reports.add(ReportType.BY_PAYEE)
            }
            if (MyPreferences.isShowLocation(requireActivity())) {
                reports.add(ReportType.BY_LOCATION)
            }
            if (MyPreferences.isShowProject(requireActivity())) {
                reports.add(ReportType.BY_PROJECT)
            }
            reports.add(ReportType.BY_ACCOUNT_BY_PERIOD)
            reports.add(ReportType.BY_CATEGORY_BY_PERIOD)
            if (MyPreferences.isShowPayee(requireActivity())) {
                reports.add(ReportType.BY_PAYEE_BY_PERIOD)
            }
            if (MyPreferences.isShowLocation(requireActivity())) {
                reports.add(ReportType.BY_LOCATION_BY_PERIOD)
            }
            if (MyPreferences.isShowProject(requireActivity())) {
                reports.add(ReportType.BY_PROJECT_BY_PERIOD)
            }
        }
        return reports.toTypedArray()
    }
}