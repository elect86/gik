package main.operation

import main.Commit
import main.SimpleGit
import main.Tag
import main.plusAssign
import kotlin.test.BeforeTest
import kotlin.test.Test

class TagList_test : SimpleGit() {
    
    val commits = ArrayList<Commit>()
    val tags = ArrayList<Tag>()

    @BeforeTest
    override fun setup() {
        super.setup()
        repoFile("1.txt") += "1"
        commits += gik.commit(message= "do", all= true)
        tags += gik.tag.add(name= "tag1", message= "My message")

        repoFile("1.txt") += "2"
        commits += gik.commit(message= "do", all= true)
        tags += gik.tag.add(name= "tag2", message= "My other\nmessage")

        tags += gik.tag.add(name= "tag3", message= "My next message.", pointsTo= "tag1")
    }

    @Test
    fun `tag list lists all tags`() = assert(gik.tag.list() == tags)
}
