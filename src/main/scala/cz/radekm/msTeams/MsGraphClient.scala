package cz.radekm.msTeams

import com.microsoft.aad.msal4j.{DeviceCode, DeviceCodeFlowParameters, IAuthenticationResult, PublicClientApplication}
import com.microsoft.graph.authentication.IAuthenticationProvider
import com.microsoft.graph.core.DefaultClientConfig
import com.microsoft.graph.http.{IBaseCollectionPage, IHttpRequest, IRequestBuilder}
import com.microsoft.graph.httpcore.{HttpClients, ICoreAuthenticationProvider}
import com.microsoft.graph.logger.{DefaultLogger, LoggerLevel}
import com.microsoft.graph.models.extensions.{Channel, Chat, ChatMessage, ConversationMember, Team, User}
import com.microsoft.graph.requests.extensions.GraphServiceClient
import okhttp3.Request

import java.util.concurrent.{CompletableFuture, TimeUnit}
import java.util.function.Consumer
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters._

private class SimpleCoreAuthProvider(accessToken: String) extends ICoreAuthenticationProvider {
  override def authenticateRequest(request: Request): Request =
    request.newBuilder().addHeader("Authorization", "Bearer " + accessToken).build()
}

private class SimpleAuthProvider(accessToken: String) extends IAuthenticationProvider {
  override def authenticateRequest(request: IHttpRequest): Unit =
    request.addHeader("Authorization", "Bearer " + accessToken)
}

object MsGraphClient {
  private val authority = "https://login.microsoftonline.com/common/"

  def obtainUserAccessTokenViaTerminal(appId: AppId, scopes: Set[String]): CompletableFuture[IAuthenticationResult] = {
    val app = PublicClientApplication.builder(appId).authority(authority).build
    val showInstructions: Consumer[DeviceCode] = deviceCode => println(deviceCode.message())
    app.acquireToken(DeviceCodeFlowParameters.builder(scopes.asJava, showInstructions).build)
  }
}

class MsGraphClient(accessToken: Token) {
  private val authProvider = new SimpleAuthProvider(accessToken)
  private val coreAuthProvider = new SimpleCoreAuthProvider(accessToken)

  private val graphClient = {
    val logger = new DefaultLogger
    logger.setLoggingLevel(LoggerLevel.ERROR)

    val httpClient = HttpClients.createDefault(coreAuthProvider).newBuilder
      .connectTimeout(60_000, TimeUnit.MILLISECONDS)
      .readTimeout(60_000, TimeUnit.MILLISECONDS)
      .retryOnConnectionFailure(true)
      .build

    val httpProvider = DefaultClientConfig.createWithAuthenticationProvider(authProvider).getHttpProvider(httpClient)

    GraphServiceClient.builder()
      .authenticationProvider(authProvider)
      .logger(logger)
      .httpProvider(httpProvider)
      .buildClient
  }

  def user: User = graphClient.me.buildRequest().get()

  private def downloadAllPages[Item, RequestBuilder <: IRequestBuilder](
    firstPage: IBaseCollectionPage[Item, RequestBuilder]
  )(
    getNextPage: RequestBuilder => IBaseCollectionPage[Item, RequestBuilder]
  ): List[Item] = {
    val result = ListBuffer[Item]()
    var page = firstPage
    while (true) {
      page.getCurrentPage.forEach(msg => result += msg)

      page.getNextPage match {
        case null => return result.toList
        case next => page = getNextPage(next)
      }
    }
    sys.error("Absurd")
  }

  def teams: List[Team] = {
    val firstPage = graphClient.me.joinedTeams().buildRequest().get()
    downloadAllPages(firstPage)(_.buildRequest().get())
  }

  def chats: List[Chat] = {
    val firstPage = graphClient.me.chats().buildRequest().get()
    downloadAllPages(firstPage)(_.buildRequest().get())
  }

  def chatMembers(chatId: String): List[ConversationMember] = {
    val firstPage = graphClient.chats(chatId).members().buildRequest().get()
    downloadAllPages(firstPage)(_.buildRequest().get())
  }

  def chatMessages(chatId: String): List[ChatMessage] = {
    val firstPage = graphClient.me.chats(chatId).messages().buildRequest().get()
    downloadAllPages(firstPage)(_.buildRequest().get())
  }

  // Note: There are no replies in chats.

  def channels(teamId: String): List[Channel] = {
    val firstPage = graphClient.teams(teamId).channels().buildRequest().get()
    downloadAllPages(firstPage)(_.buildRequest().get())
  }

  // Not enough permissions to try that :-(
  def channelMembers(teamId: String, channelId: String): List[ConversationMember] = {
    val firstPage = graphClient.teams(teamId).channels(channelId).members().buildRequest().get()
    downloadAllPages(firstPage)(_.buildRequest().get())
  }

  def channelMessages(teamId: String, channelId: String): List[ChatMessage] = {
    val firstPage = graphClient.teams(teamId).channels(channelId).messages().buildRequest().get()
    downloadAllPages(firstPage)(_.buildRequest().get())
  }

  def channelMessageReplies(teamId: String, channelId: String, messageId: String): List[ChatMessage] = {
    val firstPage = graphClient.teams(teamId).channels(channelId).messages(messageId).replies().buildRequest().get()
    downloadAllPages(firstPage)(_.buildRequest().get())
  }
}
