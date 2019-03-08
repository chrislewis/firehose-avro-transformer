package com.meetup.aws.lambda.util

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicSessionCredentials}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.amazonaws.services.securitytoken.model.{AssumeRoleRequest, Credentials, GetSessionTokenRequest, GetSessionTokenResult}
import com.amazonaws.services.securitytoken.{AWSSecurityTokenService, AWSSecurityTokenServiceClientBuilder}

import java.util.UUID.randomUUID
import java.time.{LocalDateTime, ZoneId}


trait S3Client {
  def client: AmazonS3
}

object S3Client{
  case class FancyAssumedRoleS3Client(roleARN: String, clientRegion: String) extends S3Client {
    private var _credentials: Credentials = generateNewCredentialsForAssumedRole
    private var _client: AmazonS3 = constructClientWithCredentials(_credentials)
    
    def generateNewCredentialsForAssumedRole(): Credentials = {
      val roleSessionName: String = randomUUID().toString
      val stsClient : AWSSecurityTokenService = AWSSecurityTokenServiceClientBuilder.standard.withRegion(clientRegion).build()
      val roleRequest: AssumeRoleRequest = new AssumeRoleRequest().withRoleArn(roleARN).withRoleSessionName(roleSessionName)
      stsClient.assumeRole(roleRequest).getCredentials()
    }

    def constructClientWithCredentials(credentials: Credentials): AmazonS3 = {
      AmazonS3ClientBuilder.standard()
        .withCredentials(new AWSStaticCredentialsProvider(
          new BasicSessionCredentials(
          _credentials.getAccessKeyId, 
          _credentials.getSecretAccessKey, 
          _credentials.getSessionToken)
        ))
        .withRegion(clientRegion).build()
    }

    def credentialsAreAboutToExpire: Boolean = {
      val credentialExperation = LocalDateTime.ofInstant(_credentials.getExpiration.toInstant, ZoneId.systemDefault); 
      credentialExperation.isBefore(LocalDateTime.now.plusMinutes(5))
    }

    def renewCredentialsAndConstructNewClient: Unit = {
      _credentials = generateNewCredentialsForAssumedRole
      _client = constructClientWithCredentials(_credentials)
    }
    
    def client: AmazonS3 = {
      if(credentialsAreAboutToExpire){
        renewCredentialsAndConstructNewClient
      }
      _client
    }
  }

  class BasicS3Client extends S3Client{
    val client: AmazonS3 = AmazonS3ClientBuilder.defaultClient()
  }

  def apply(): S3Client = sys.env.get("ROLE_ARN") match {
    case Some(roleARN) => 
      new FancyAssumedRoleS3Client(roleARN, sys.env.getOrElse("REGION", "us-east-1"))
    case None => 
      new BasicS3Client()
 }
}
