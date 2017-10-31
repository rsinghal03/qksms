package com.moez.QKSMS.presentation.conversations

import android.Manifest
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.LinearLayoutManager
import com.jakewharton.rxbinding2.view.RxView
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.moez.QKSMS.R
import com.moez.QKSMS.common.di.AppComponentManager
import com.moez.QKSMS.presentation.Navigator
import com.moez.QKSMS.presentation.base.QkActivity
import kotlinx.android.synthetic.main.conversation_list_activity.*
import kotlinx.android.synthetic.main.drawer_view.*
import kotlinx.android.synthetic.main.toolbar.*
import timber.log.Timber
import javax.inject.Inject

class ConversationsActivity : QkActivity(), ConversationsView {

    @Inject lateinit var navigator: Navigator

    private lateinit var viewModel: ConversationsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppComponentManager.appComponent.inject(this)
        setContentView(R.layout.conversation_list_activity)
        ActionBarDrawerToggle(this, drawerLayout, toolbar, 0, 0).syncState()

        onNewIntent(intent)
        requestPermissions()

        viewModel = ViewModelProviders.of(this)[ConversationsViewModel::class.java]
        viewModel.state.observe(this, Observer { it?.let { render(it) } })
        viewModel.view = this

        conversationList.layoutManager = LinearLayoutManager(this)

        swipeRefresh.setOnRefreshListener { viewModel.onRefresh() }
        RxView.clicks(compose).subscribe { }
        RxView.clicks(drawer).subscribe { }
        RxView.clicks(archived).subscribe { }
        RxView.clicks(scheduled).subscribe { }
        RxView.clicks(blocked).subscribe { }
        RxView.clicks(settings).subscribe { }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.getLongExtra("threadId", 0)
                ?.takeIf { threadId -> threadId > 0 }
                ?.let { threadId -> navigator.showConversation(threadId) }
    }

    override fun render(state: ConversationsState) {
        if (conversationList.adapter == null && state.conversations?.isValid == true) {
            conversationList.adapter = ConversationsAdapter(state.conversations)
        }

        swipeRefresh.isRefreshing = state.refreshing
    }

    private fun requestPermissions() {
        Dexter.withActivity(this)
                .withPermissions(arrayListOf(Manifest.permission.READ_CONTACTS, Manifest.permission.READ_SMS))
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionRationaleShouldBeShown(request: MutableList<PermissionRequest>, token: PermissionToken) {
                    }

                    override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                        for (response in report.grantedPermissionResponses) Timber.v("Permission granted: ${response.permissionName}")
                        for (response in report.deniedPermissionResponses) Timber.v("Permission denied: ${response.permissionName}")
                    }
                })
                .check()
    }
}