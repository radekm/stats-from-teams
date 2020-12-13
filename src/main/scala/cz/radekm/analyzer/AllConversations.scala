package cz.radekm.analyzer

import cz.radekm.msTeams.Teams

case class AllConversations(channels: List[ChannelWithMessages], chats: List[ChatWithMessages])

case class ChannelWithMessages(channel: Teams.Channel, messages: List[MessageWithReplies])

/**
 * Chat doesn't have replies - we use `MessageWithReplies` to unify processing with channels.
 */
case class ChatWithMessages(chat: Teams.Chat, messages: List[MessageWithReplies])

case class MessageWithReplies(
  message: Teams.Message,
  replies: List[Teams.Message],
)
