package org.schabi.newpipe.about

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.schabi.newpipe.brave.tip.BraveTipActivity
import org.schabi.newpipe.databinding.FragmentAboutBinding

class BoolWrapper(isAlreadyAdded: Boolean) {
    var value: Boolean = isAlreadyAdded
}

open class BraveAboutFragment : Fragment() {

    protected fun braveSetupViews(
        grp: ViewGroup,
        fragmentAboutBinding: FragmentAboutBinding
    ) {
        fragmentAboutBinding.braveAbout.braveAboutTip.setOnClickListener {
            val intent = Intent(context, BraveTipActivity::class.java)
            context?.startActivity(intent)
        }

        braveSetAllNoneBravePipeViewsGone(grp, fragmentAboutBinding)
    }

    private fun braveSetAllNoneBravePipeViewsGone(
        grp: ViewGroup,
        fragmentAboutBinding: FragmentAboutBinding
    ) {
        val doSetRemainingViewsGone = BoolWrapper(false)
        braveSetAllNoneBravePipeViewsGoneRecursive(
            doSetRemainingViewsGone,
            grp,
            fragmentAboutBinding
        )
    }

    private fun braveSetAllNoneBravePipeViewsGoneRecursive(
        doSetRemainingViewsGone: BoolWrapper,
        grp: ViewGroup,
        fragmentAboutBinding: FragmentAboutBinding
    ) {
        for (x in 0 until grp.childCount) {
            val child = grp.getChildAt(x)
            if (child is ViewGroup) {
                if (child == fragmentAboutBinding.braveAbout.root) { // all later view's are not relevant for BravePipe
                    doSetRemainingViewsGone.value = true
                } else {
                    braveSetAllNoneBravePipeViewsGoneRecursive(
                        doSetRemainingViewsGone,
                        child as ViewGroup,
                        fragmentAboutBinding
                    )
                }
            } else {
                if (doSetRemainingViewsGone.value) {
                    child.visibility = View.GONE
                }
            }
        }
    }

    protected fun braveAddSoftwareComponents(softwareComponents: ArrayList<SoftwareComponent>) {
        if (areSoftwareComponentsAlreadyAdded) {
            return; // run only once
        }
        areSoftwareComponentsAlreadyAdded = true
        softwareComponents.add(
            SoftwareComponent(
                "EventBus", "2012 - 2016", "Markus Junginger",
                "https://greenrobot.org/eventbus", StandardLicenses.APACHE2
            )
        )
        softwareComponents.add(
            SoftwareComponent(
                "OsExt", "2024", "evermind-zz",
                "https://github.com/evermind-zz/OsExt", StandardLicenses.GPL3
            )
        )
        softwareComponents.add(
            SoftwareComponent(
                "HlsDownloader", "2025", "evermind-zz",
                "https://github.com/evermind-zz/HlsDownloader", StandardLicenses.GPL3
            )
        )
        softwareComponents.add(
            SoftwareComponent(
                "slimhls-converter", "2025", "evermind-zz",
                "https://github.com/evermind-zz/slimhls-converter", StandardLicenses.GPL3
            )
        )
        softwareComponents.add(
            SoftwareComponent(
                "logcatviewer", "2017 - 2026", "kyze8439690 (evermind-zz: fork)",
                "https://github.com/evermind-zz/logcatviewer", StandardLicenses.APACHE2
            )
        )
        softwareComponents.add(
            SoftwareComponent(
                "challengeFloatsAway", "2026", "evermind-zz",
                "https://github.com/evermind-zz/challengeFloatsAway", StandardLicenses.GPL3
            )
        )
    }

    companion object {
        private var areSoftwareComponentsAlreadyAdded: Boolean = false
    }
}
