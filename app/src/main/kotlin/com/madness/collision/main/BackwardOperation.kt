/*
 * Copyright 2020 Clifford Liu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.madness.collision.main

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentManager
import com.madness.collision.util.Id
import com.madness.collision.util.TaggedFragment
import java.lang.ref.WeakReference
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

class BackwardOperation(val operationFlags: BooleanArray): Parcelable {

    class Page(private val uid: String, private val clazz: KClass<*>): Parcelable {
        private var args: Bundle? = null
        private var ref: WeakReference<TaggedFragment?> = WeakReference(null)

        val hasRef: Boolean
            get() = ref.get() != null
        val fragment: TaggedFragment
            get() = ref.get() ?: newFragment
        val refFragment: TaggedFragment?
            get() = ref.get()
        val newFragment: TaggedFragment
            get() = (clazz.createInstance() as TaggedFragment).apply {
                arguments = args
            }

        constructor(fragment: TaggedFragment): this(fragment.uid, fragment::class) {
            ref = WeakReference(fragment)
            args = fragment.arguments
        }

        fun tryToEnsure(fragmentManager: FragmentManager): Page {
            if (hasRef) return this
            return tryToFind(fragmentManager)
        }

        fun tryToFind(fragmentManager: FragmentManager): Page {
            val f = fragmentManager.findFragmentByTag(uid)
                    // fragments managed by navigation component have no tag set
                    ?: fragmentManager.fragments.find { it is Id && it.uid == uid }
                            ?: return this
            ref = WeakReference(f as TaggedFragment)
            return this
        }

        constructor(parcel: Parcel) : this(parcel.readString() ?: "", Class.forName(parcel.readString() ?: "").kotlin) {
            args = parcel.readBundle(javaClass.classLoader)
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(uid)
            parcel.writeString(clazz::qualifiedName.name)
            parcel.writeBundle(args)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<Page> {
            override fun createFromParcel(parcel: Parcel): Page {
                return Page(parcel)
            }

            override fun newArray(size: Int): Array<Page?> {
                return arrayOfNulls(size)
            }
        }
    }

    private lateinit var _forwardPage: Page
    private lateinit var _backwardPage: Page
    val forwardPage: Page
        get() = _forwardPage
    val backwardPage: Page
        get() = _backwardPage

    var cachedCallback: OnBackPressedCallback? = null

    constructor(forwardFragment: TaggedFragment, backwardFragment: TaggedFragment, operationFlags: BooleanArray): this(operationFlags) {
        _forwardPage = Page(forwardFragment)
        _backwardPage = Page(backwardFragment)
    }

    fun tryToEnsure(fragmentManager: FragmentManager): BackwardOperation {
        forwardPage.tryToEnsure(fragmentManager)
        backwardPage.tryToEnsure(fragmentManager)
        return this
    }

    constructor(parcel: Parcel) : this(parcel.createBooleanArray() ?: booleanArrayOf()) {
        val cl = javaClass.classLoader
        _forwardPage = parcel.readParcelable(cl)!!
        _backwardPage = parcel.readParcelable(cl)!!
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeBooleanArray(operationFlags)
        parcel.writeParcelable(forwardPage, flags)
        parcel.writeParcelable(backwardPage, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<BackwardOperation> {
        override fun createFromParcel(parcel: Parcel): BackwardOperation {
            return BackwardOperation(parcel)
        }

        override fun newArray(size: Int): Array<BackwardOperation?> {
            return arrayOfNulls(size)
        }
    }
}
