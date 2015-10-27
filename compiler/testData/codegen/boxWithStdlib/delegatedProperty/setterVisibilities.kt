import kotlin.properties.Delegates

open class A {
    private var privatePropertyPrivateSet: Int by Delegates.notNull()
        private set

    private var privatePropertyProtectedSet: Int by Delegates.notNull()
        protected set

    private var privatePropertyInternalSet: Int by Delegates.notNull()
        internal set

    private var privatePropertyPublicSet: Int by Delegates.notNull()
        public set

    protected var protectedPropertyPrivateSet: Int by Delegates.notNull()
        private set

    protected var protectedPropertyProtectedSet: Int by Delegates.notNull()
        protected set

    protected var protectedPropertyInternalSet: Int by Delegates.notNull()
        internal set

    protected var protectedPropertyPublicSet: Int by Delegates.notNull()
        public set

    internal var internalPropertyPrivateSet: Int by Delegates.notNull()
        private set

    internal var internalPropertyProtectedSet: Int by Delegates.notNull()
        protected set

    internal var internalPropertyInternalSet: Int by Delegates.notNull()
        internal set

    internal var internalPropertyPublicSet: Int by Delegates.notNull()
        public set

    public var publicPropertyPrivateSet: Int by Delegates.notNull()
        private set

    public var publicPropertyProtectedSet: Int by Delegates.notNull()
        protected set

    public var publicPropertyInternalSet: Int by Delegates.notNull()
        internal set

    public var publicPropertyPublicSet: Int by Delegates.notNull()
        public set

    init {
        privatePropertyPrivateSet = 1
        privatePropertyProtectedSet = 1
        privatePropertyInternalSet = 1
        privatePropertyPublicSet = 1

        protectedPropertyPrivateSet = 1
        protectedPropertyProtectedSet = 1
        protectedPropertyInternalSet = 1
        protectedPropertyPublicSet = 1

        internalPropertyPrivateSet = 1
        internalPropertyProtectedSet = 1
        internalPropertyInternalSet = 1
        internalPropertyPublicSet = 1

        publicPropertyPrivateSet = 1
        publicPropertyProtectedSet = 1
        publicPropertyInternalSet = 1
        publicPropertyPublicSet = 1
    }

    override fun toString(): String = """
        privatePropertyPrivateSet = $privatePropertyPrivateSet
        privatePropertyProtectedSet = $privatePropertyProtectedSet
        privatePropertyInternalSet = $privatePropertyInternalSet
        privatePropertyPublicSet = $privatePropertyPublicSet

        protectedPropertyPrivateSet = $protectedPropertyPrivateSet
        protectedPropertyProtectedSet = $protectedPropertyProtectedSet
        protectedPropertyInternalSet = $protectedPropertyInternalSet
        protectedPropertyPublicSet = $protectedPropertyPublicSet

        internalPropertyPrivateSet = $internalPropertyPrivateSet
        internalPropertyProtectedSet = $internalPropertyProtectedSet
        internalPropertyInternalSet = $internalPropertyInternalSet
        internalPropertyPublicSet = $internalPropertyPublicSet

        publicPropertyPrivateSet = $publicPropertyPrivateSet
        publicPropertyProtectedSet = $publicPropertyProtectedSet
        publicPropertyInternalSet = $publicPropertyInternalSet
        publicPropertyPublicSet = $publicPropertyPublicSet""".trimIndent()
}

class B : A() {
    init {
        protectedPropertyProtectedSet = 2
        protectedPropertyInternalSet = 2
        protectedPropertyPublicSet = 2

        internalPropertyProtectedSet = 2
        internalPropertyInternalSet = 2
        internalPropertyPublicSet = 2

        publicPropertyProtectedSet = 2
        publicPropertyInternalSet = 2
        publicPropertyPublicSet = 2
    }
}

fun box(args: Array<String>) {
    val b = B()

    b.internalPropertyInternalSet = 3
    b.internalPropertyPublicSet = 3

    b.publicPropertyInternalSet = 3
    b.publicPropertyPublicSet = 3

    val actual = b.toString()
    val expected = """
        privatePropertyPrivateSet = 1
        privatePropertyProtectedSet = 1
        privatePropertyInternalSet = 1
        privatePropertyPublicSet = 1

        protectedPropertyPrivateSet = 1
        protectedPropertyProtectedSet = 2
        protectedPropertyInternalSet = 2
        protectedPropertyPublicSet = 2

        internalPropertyPrivateSet = 1
        internalPropertyProtectedSet = 1
        internalPropertyInternalSet = 3
        internalPropertyPublicSet = 3

        publicPropertyPrivateSet = 1
        publicPropertyProtectedSet = 2
        publicPropertyInternalSet = 3
        publicPropertyPublicSet = 3""".trimIndent()

    if (actual != expected) throw Exception("Test failed. Actual:\n$actual\nExpected:\n$expected")
}