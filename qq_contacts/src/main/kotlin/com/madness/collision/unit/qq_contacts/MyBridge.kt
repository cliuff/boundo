package com.madness.collision.unit.qq_contacts

import com.madness.collision.unit.Bridge
import com.madness.collision.unit.Unit

object MyBridge: Bridge() {

    override val unitName: String = Unit.UNIT_NAME_QQ_CONTACTS

    /**
     * @param args empty
     */
    override fun getUnitInstance(vararg args: Any?): Unit {
        return MyUnit()
    }
}
