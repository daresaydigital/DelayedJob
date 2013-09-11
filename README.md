DelayedJob
==========
DelayedJob is an android library used for performing a job in the future. There are ways to do this using the standard SDK componenets but DelayedJob makes it both easier to implement and adds some awesome features! Read below to find out why and how to use it :)

DelayedJobs are `Intent`s that are scheduled to be run in the future. This works much like Androids `AlarmManager` but with a couple of important differences. The job (an intent) is only scheduled while your application in active. DelayedJob handles retrying of jobs and canceling pending jobs of the same kind (if you want).

Installation
------------
Clone the library via `git clone github.com/screeninteraction/delayedjob`. After this is done you can choose to either build it and include the jar as a dependecy in your application or you can import it as a android library project and add it to your project that way.

Usage
-----
First of all you must call `DelayedJob.init(Context)` when your application starts. This can either be done in your main activity or in a custom application class.

One usage scenario for DelayedJob is if you have an application with posts, where each post can be liked, much like facebook or instagram. To mitigate server load from users spamming a like button you can use DelayedJob to send there requests to the server.
```java
public void setLikeStatus(Post post, boolean likeStatus){
	Intent i = new Intent();
	i.putExtra("like_status", likeStatus)
	new DelayedJob(LikeJobHandler.class, i)
		.withDelay(10 * 1000) // Wait 10 seconds to perform this job
		.withId(post.id) // Use this to identify the job.
		.replacePrevious(true) // If there is a pending job with the same id and handler, remove it.
		.withRetryCount(10) // Try a maximum of 10 times to perform this job
		.withRetryDelay(5 * 60 * 10000) // Wait 5 minutes between tries
		.withTimeOut(System.currentTimeMillis() + 24 * 60 * 60 * 10000) // Time out the request after 24 hours
		.perform(); // The job will be performed either now or later, depending on the delay set.
}
```
All the above methods are preset with sensible defaults so you only need to call the ones you are interested in setting. The above request will make sure that even though the user spams the like button for one post, the server will only recieve the final request. This is because the job is set at a delay of 10 seconds and will delete any pending jobs with the same id (the id of the post) when inserting a new job.

The `LikeJobHandler` will look something like this.
```java
public class LikeJobHandler extends DelayedJobHandler {

	public boolean performJob(Context context, Intent job) {
		if (hasInternet()) {
			try {
				doStuff(job);
				return true;
			} catch (Exception e) {
				return false;
			}
		}
		return false;
	}

}
```
The `LikeJobHandler` will make the request and return true if is succeeded, If it returns false it means that is did not succeed and it should be retried if it has any retries left. The `LikeJobHandler.performJob()` will always be run off of the main thread so it is up to you to post messages onto the main thread if this is something you need to do.

Contributing
------------
###Issues
- Use a short and descriptive title.
- Include any crash logs found in log cat.
- Give a detailed description of the bug or if it's a feature request please describe why this should be added to the library and what the use cases are.

##Pull requests
- Pull requests should contain clean and discriptive commit messages.
- If you are fixing a bug, describe how to reproduce the bug and what you did to fix it.
- If you are adding a feature, describe why this feature should be added and what the use cases for it is.
- List any side effects of your pull request. 
