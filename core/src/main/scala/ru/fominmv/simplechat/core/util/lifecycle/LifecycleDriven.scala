package ru.fominmv.simplechat.core.util.lifecycle


trait LifecycleDriven extends Openable:
    def lifecyclePhase: LifecyclePhase

    override def closed: Boolean =
        lifecyclePhase == LifecyclePhase.CLOSING ||
        lifecyclePhase == LifecyclePhase.CLOSED