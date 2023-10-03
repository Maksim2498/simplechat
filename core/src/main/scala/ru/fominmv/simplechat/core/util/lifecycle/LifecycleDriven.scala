package ru.fominmv.simplechat.core.util.lifecycle


import LifecyclePhase.*


trait LifecycleDriven extends Openable:
    def lifecyclePhase: LifecyclePhase

    def canOpen: Boolean =
        lifecyclePhase == NEW

    def canClose: Boolean =
        lifecyclePhase == OPEN

    override def closed: Boolean =
        lifecyclePhase == CLOSING ||
        lifecyclePhase == CLOSED