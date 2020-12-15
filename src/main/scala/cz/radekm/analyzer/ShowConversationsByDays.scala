package cz.radekm.analyzer

import cz.radekm.msTeams.{@@, TaggingExts, Teams}
import monix.eval.Task

import java.nio.file.Paths
import java.time.{LocalDate, ZoneId}
import scala.math.Ordered.orderingToOrdered

object ShowConversationsByDays {
  def messagePredicate(userId: String @@ "User", day: LocalDate): Teams.Message => Boolean = m => {
    // Work day ends at 4 am.
    val from = day.atStartOfDay(ZoneId.systemDefault()).plusHours(4).toInstant
    val to = day.plusDays(1).atStartOfDay(ZoneId.systemDefault()).plusHours(4).toInstant
    m.author.exists(_.id == userId) && from <= m.created && m.created < to
  }

  def retainByPredicate(predicate: Teams.Message => Boolean)(
    m: MessageWithReplies
  ): Option[MessageWithReplies] = {
    val replies = m.replies.filter(predicate)
    if (replies.nonEmpty || predicate(m.message)) {
      Some(m.copy(replies = replies))
    } else None
  }

  def retainByPredicateForChannel(predicate: Teams.Message => Boolean)(
    channel: ChannelWithMessages
  ): Option[ChannelWithMessages] = {
    val messages = channel.messages.flatMap(retainByPredicate(predicate)(_))
    if (messages.nonEmpty) {
      Some(channel.copy(messages = messages))
    } else None
  }

  def retainByPredicateForChat(predicate: Teams.Message => Boolean)(
    chat: ChatWithMessages
  ): Option[ChatWithMessages] = {
    val messages = chat.messages.flatMap(retainByPredicate(predicate)(_))
    if (messages.nonEmpty) {
      Some(chat.copy(messages = messages))
    } else None
  }

  def retainByPredicateForConversations(predicate: Teams.Message => Boolean)(
    conversations: AllConversations
  ): AllConversations = AllConversations(
    channels = conversations.channels.flatMap(retainByPredicateForChannel(predicate)),
    chats = conversations.chats.flatMap(retainByPredicateForChat(predicate))
  )

  def main(args: Array[String]): Unit = {
    val userId = args.headOption.getOrElse(sys.error("User id not defined")).tagWith["User"]
    val from = LocalDate.parse(args.drop(1).headOption.getOrElse(sys.error("From not defined")))
    val to = LocalDate.parse(args.drop(2).headOption.getOrElse(sys.error("To not defined")))
    val inputFile = args.drop(3).headOption.getOrElse("conversations.json")

    val days = List.unfold(0) { st =>
      val current = from.plusDays(st)
      if (current <= to) Some(current, st + 1)
      else None
    }

    val program = for {
      conversations <- Json.loadFromFile(Paths.get(inputFile))
      perDay = days.map { day =>
        val predicate = messagePredicate(userId, day)
        day -> retainByPredicateForConversations(predicate)(conversations)
      }
      _ <- Task.traverse(perDay) { case (day, conversations) => Task {
        println(s"Day $day =============")
        println(conversations)
      } }
    } yield ()

    import monix.execution.Scheduler.Implicits.global
    program.runSyncUnsafe()
  }
}
