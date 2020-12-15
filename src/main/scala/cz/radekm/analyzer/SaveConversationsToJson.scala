package cz.radekm.analyzer

import cz.radekm.msTeams._
import monix.eval.Task

import java.nio.file.Paths

object SaveConversationsToJson {
  def downloadChannel(client: TeamsClient, channel: Teams.Channel): Task[ChannelWithMessages] =
    for {
      messages <- client.messages(channel)
      messagesWithReplies <- Task.traverse(messages) { message =>
        client.repliesToMessage(channel, message).map { replies => MessageWithReplies(message, replies) }
      }
    } yield ChannelWithMessages(channel, messagesWithReplies)

  def downloadChat(client: TeamsClient, chat: Teams.Chat): Task[ChatWithMessages] =
    for {
      messages <- client.messages(chat)
      messagesWithReplies = messages.map(MessageWithReplies(_, Nil))
    } yield ChatWithMessages(chat, messagesWithReplies)

  def downloadAllConversations(client: TeamsClient): Task[AllConversations] =
    for {
      _ <- Task { println("Downloading channels") }
      channels <- client.channels
      channelsWithMessages <- Task.traverse(channels) { channel =>
        Task { println(s"Downloading channel $channel") }.flatMap { _ =>
          downloadChannel(client, channel)
        }
      }
      _ <- Task { println("Downloading chats") }
      chats <- client.chats
      chatsWithMessages <- Task.traverse(chats) { chat =>
        Task { println(s"Downloading chat $chat") }.flatMap { _ =>
          downloadChat(client, chat)
        }
      }
      _ <- Task { println("Conversations downloaded") }
    } yield AllConversations(channelsWithMessages, chatsWithMessages)

  def main(args: Array[String]): Unit = {
    if (args.size < 1)
      sys.error("Must have at least one argument: appId")

    val appId = args.head.tagWith["AppId"]
    val outputFile = args.drop(1).headOption.getOrElse("conversations.json")

    val program = for {
      client <- TeamsClient.make(appId)
      conversations <- downloadAllConversations(client)
      _ <- Json.saveToFile(Paths.get(outputFile), conversations)
    } yield ()

    import monix.execution.Scheduler.Implicits.global
    program.runSyncUnsafe()
  }
}
