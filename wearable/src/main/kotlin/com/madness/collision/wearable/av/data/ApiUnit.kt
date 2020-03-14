package com.madness.collision.wearable.av.data

class ApiUnit : ArrayList<Int>() {
    companion object {
        const val NON = 0
        const val USER = 1
        const val SYS = 2
        const val ALL_APPS = 3

        fun ineffective(item: Int) : Boolean = item !in 1..7
    }

    private val loading = ArrayList<Int>()

    fun isLoading(item: Int): Boolean{
        when(item){
            USER, SYS -> return loading.contains(item)
            ALL_APPS -> {
                val loadedUser = contains(USER)
                val loadedSys = contains(SYS)
                if (loadedUser && !loadedSys){
                    if (loading.contains(SYS)) return true
                }else if (!loadedUser && loadedSys){
                    if (loading.contains(USER)) return true
                }
            }
        }
        return false
    }

    fun loading(item: Int){
        when(item){
            USER, SYS -> loading.add(item)
            ALL_APPS -> {
                if (!loading.contains(USER)) loading.add(USER)
                if (!loading.contains(SYS)) loading.add(SYS)
            }
        }
    }

    fun finish(item : Int){
        when(item){
            USER, SYS -> {
                add(item)
                if (loading.contains(item))
                    loading.remove(item)
            }
            ALL_APPS -> {
                if (!contains(USER)) finish(USER)
                if (!contains(SYS)) finish(SYS)
            }
        }
    }

    fun shouldLoad(item : Int) : Boolean = when(item){
        USER -> !contains(USER) && !isLoading(USER)
        SYS -> !contains(SYS) && !isLoading(SYS)
        ALL_APPS -> !(contains(USER) && contains(SYS)) && !isLoading(ALL_APPS)
        NON -> false
        else -> true
    }

    fun item2Load() : Int{
        if (shouldLoad(USER)) return USER
        if (shouldLoad(SYS)) return SYS
        return NON
    }

    fun unLoad(unit : Int){
        when(unit){
            USER -> remove(USER)
            SYS -> remove(SYS)
            ALL_APPS -> {
                if (contains(USER)) remove(USER)
                if (contains(SYS)) remove(SYS)
            }
        }
    }

    fun allLoaded() : Boolean = contains(USER) && contains(SYS)
}