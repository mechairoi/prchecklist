package prchecklist.web

import org.mockito.MockSettings
import org.scalatest.{ Matchers, OptionValues, mock }
import org.scalatra.test.scalatest._
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.mockito.Matchers.{ eq => matchEq }

import prchecklist.{ MyScalatraServlet }
import prchecklist.models._
import prchecklist.utils._

import scalaz.\/-

class ServletSpec extends ScalatraFunSuite with Matchers with OptionValues with mock.MockitoSugar {
  addServlet(new MyScalatraServlet {
    put("/@user") {
      session("userLogin") = params("login")
      session("accessToken") = ""
    }

    override def httpUtils = {
      val hu = mock[HttpUtils]

      import JsonTypes._

      def stubJson[A](url: String, data: A) {
        when(hu.httpRequestJson[A](matchEq(url), any())(any(), any()))
          .thenReturn(\/-(data))
      }

      val repo = GitHubRepo(
        fullName = "motemen/test-repository",
        `private` = false,
        url = "<url>"
      )
      stubJson(
        "https://api.github.com/repos/motemen/test-repository/pulls/2",
        GitHubPullRequest(
          number = 1,
          url = "url",
          title = "title",
          body = "body",
          head = GitHubCommitRef(
            repo = repo,
            sha = "",
            ref = "feature-1"
          ),
          base = GitHubCommitRef(
            repo = repo,
            sha = "",
            ref = "master"
          )
        )
      )

      stubJson(
        "https://api.github.com/repos/motemen/test-repository/pulls/2/commits?per_page=100",
        List(
          GitHubCommit(
            sha = "",
            commit = GitHubCommitDetail(
              """Merge pull request #1 from motemen/feature-1
                |
                |feature-1
              """.stripMargin
            )
          ),
          GitHubCommit(
            sha = "",
            commit = GitHubCommitDetail("Implement feature-1")
          )
        )
      )

      hu
    }
  }, "/*")

  test("index") {
    get("/") {
      status should equal (200)
    }
  }

  test("viewPullRequest") {
    session {
      get("/motemen/test-repository/pull/2") {
        status should equal (302)
        println(header.get("Location").map {
          loc => new java.net.URI(loc).getPath
        })
      }

      put("/@user?login=test-user") {
        status should equal (200)
      }

      get("/motemen/test-repository/pull/2") {
        status should equal (200)
      }
    }
  }
}