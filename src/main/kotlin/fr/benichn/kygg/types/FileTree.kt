package fr.benichn.kygg.types

sealed class FileTree {
    abstract val name: String
    abstract val size: Long
    data class File(
        override val name: String,
        override val size: Long
    ): FileTree()
    data class Directory(
        override val name: String,
        val children: List<FileTree>
    ): FileTree() {
        override val size: Long
            get() = children.sumOf { t -> t.size }
    }

    companion object {
        fun fromFiles(files: List<FileInfo>): Directory {
            val root = Directory(
                "",
                mutableListOf()
            )
            fun makeDirs(dirs: List<String>): Directory {
                var currentDir = root
                for (dir in dirs) {
                    val newDir = currentDir.children.firstOrNull { t -> t.name == dir } as Directory?
                    currentDir = newDir ?: Directory(dir, mutableListOf()).also { (currentDir.children as MutableList<FileTree>).add(it) }
                }
                return currentDir
            }
            for (fi in files) {
                val path = fi.name.split('/')
                if (path.size == 1) {
                    (root.children as MutableList<FileTree>).add(File(fi.name, fi.size))
                } else {
                    val dirs = path.dropLast(1)
                    val dir = makeDirs(dirs)
                    val file = path.last()
                    (dir.children as MutableList<FileTree>).add(File(file, fi.size))
                }
            }
            return root
        }
    }
}