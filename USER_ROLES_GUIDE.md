# User Roles System Guide

This guide explains the two-role system implemented for the Peru Work platform, which connects clients who need jobs done with craftsmen who can perform the work.

## Overview

The system supports two distinct user roles:

1. **CLIENT** - Users who need work done (e.g., homeowners, businesses)
2. **CRAFTSMAN** - Skilled workers who can perform jobs (e.g., plumbers, electricians, carpenters)

## Architecture

### Backend (Convex)

#### Schema Changes

**Users Table** ([`convex-backend/convex/schema.ts`](convex-backend/convex/schema.ts:5))
- Unified user table supporting both CLIENT and CRAFTSMAN roles
- `role` field: `"CLIENT" | "CRAFTSMAN"`
- Common fields: name, email, phone, location, profileImageUrl
- Craftsman-specific fields (optional): craft, rating, experience, description, bio, specialties, completedProjects, reviewCount
- Indexes: `by_email`, `by_role`

**Jobs Table** ([`convex-backend/convex/schema.ts`](convex-backend/convex/schema.ts:33))
- `clientId`: References the user who created the job
- `assignedCraftsmanId`: References the craftsman assigned to the job
- `status`: `"OPEN" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED"`
- Additional fields: budget, deadline, createdAt
- Legacy `craftsmanId` field maintained for backward compatibility
- Indexes: `by_client`, `by_assigned_craftsman`, `by_status`

#### API Functions

**User Management** ([`convex-backend/convex/users.ts`](convex-backend/convex/users.ts))

Queries:
- `list()` - Get all users
- `getById(id)` - Get user by ID
- `getByEmail(email)` - Get user by email
- `getByRole(role)` - Get users by role (CLIENT or CRAFTSMAN)
- `getCraftsmen()` - Get all craftsmen
- `getClients()` - Get all clients

Mutations:
- `createClient(...)` - Register a new client
- `createCraftsman(...)` - Register a new craftsman with craft details
- `updateProfile(...)` - Update user profile information
- `updateRating(...)` - Update craftsman rating and stats
- `deleteUser(id)` - Delete a user

**Job Management** ([`convex-backend/convex/jobs.ts`](convex-backend/convex/jobs.ts))

Queries:
- `list()` - Get all jobs
- `getById(id)` - Get job by ID
- `getByClientId(clientId)` - Get jobs created by a client
- `getByAssignedCraftsman(craftsmanId)` - Get jobs assigned to a craftsman
- `getByStatus(status)` - Get jobs by status
- `getOpenJobs()` - Get all available jobs

Mutations:
- `createJob(...)` - Client creates a new job
- `assignCraftsman(jobId, craftsmanId)` - Assign craftsman to job
- `updateStatus(jobId, status)` - Update job status
- `updateJob(...)` - Update job details
- `deleteJob(jobId)` - Delete a job

### Android App

#### Data Models

**User** ([`android-starter/app/src/main/java/com/example/androidstarter/data/User.kt`](android-starter/app/src/main/java/com/example/androidstarter/data/User.kt))
```kotlin
data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: String, // "CLIENT" or "CRAFTSMAN"
    // ... other fields
) {
    val userRole: UserRole // Enum helper
    val isCraftsman: Boolean
    val isClient: Boolean
}
```

**Job** ([`android-starter/app/src/main/java/com/example/androidstarter/data/Job.kt`](android-starter/app/src/main/java/com/example/androidstarter/data/Job.kt))
```kotlin
data class Job(
    val id: String,
    val title: String,
    val clientId: String?,
    val assignedCraftsmanId: String?,
    val status: String?, // "OPEN" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED"
    // ... other fields
) {
    val jobStatus: JobStatus // Enum helper
    val isOpen: Boolean
    val isInProgress: Boolean
    val isCompleted: Boolean
}
```

#### Repositories

**UserRepository** ([`android-starter/app/src/main/java/com/example/androidstarter/data/UserRepository.kt`](android-starter/app/src/main/java/com/example/androidstarter/data/UserRepository.kt))
- Manages user authentication and profile data
- Methods for creating clients and craftsmen
- Query users by role, email, or ID
- Update profiles and ratings

**JobRepository** ([`android-starter/app/src/main/java/com/example/androidstarter/data/JobRepository.kt`](android-starter/app/src/main/java/com/example/androidstarter/data/JobRepository.kt))
- Manages job creation and assignment
- Query jobs by client, craftsman, or status
- Assign craftsmen to jobs
- Update job status and details

## Usage Examples

### Creating a Client

```kotlin
val userRepo = UserRepository()

val clientId = userRepo.createClient(
    name = "John Smith",
    email = "john@example.com",
    phone = "+51 999 888 777",
    location = "Lima, Peru"
)
```

### Creating a Craftsman

```kotlin
val userRepo = UserRepository()

val craftsmanId = userRepo.createCraftsman(
    name = "Maria Garcia",
    email = "maria@example.com",
    phone = "+51 988 777 666",
    location = "Lima, Peru",
    craft = "Electrician",
    description = "Licensed electrician with 10 years experience",
    experience = 10L,
    specialties = listOf("Wiring", "Panel Upgrades", "Smart Home")
)
```

### Client Creates a Job

```kotlin
val jobRepo = JobRepository()

val jobId = jobRepo.createJob(
    title = "Fix leaky faucet",
    description = "Kitchen faucet is dripping",
    location = "123 Main St, Lima",
    clientId = currentUser.id,
    budget = 150.0,
    deadline = "2025-12-01"
)
```

### Craftsman Takes a Job

```kotlin
val jobRepo = JobRepository()

// Craftsman browses open jobs
jobRepo.getOpenJobs().collect { result ->
    result.onSuccess { jobs ->
        // Display available jobs
    }
}

// Craftsman accepts a job
jobRepo.assignCraftsman(
    jobId = selectedJob.id,
    craftsmanId = currentCraftsman.id
)
// This automatically sets status to "IN_PROGRESS"
```

### Updating Job Status

```kotlin
val jobRepo = JobRepository()

// Mark job as completed
jobRepo.updateJobStatus(
    jobId = job.id,
    status = JobStatus.COMPLETED
)
```

### Querying Jobs

```kotlin
val jobRepo = JobRepository()

// Get all jobs for a client
jobRepo.getJobsByClientId(clientId).collect { result ->
    result.onSuccess { jobs ->
        // Display client's jobs
    }
}

// Get all jobs assigned to a craftsman
jobRepo.getJobsByAssignedCraftsman(craftsmanId).collect { result ->
    result.onSuccess { jobs ->
        // Display craftsman's jobs
    }
}
```

## User Flows

### Client Flow
1. Client registers with email, phone, and location
2. Client creates a job with description, location, and optional budget/deadline
3. Job is set to "OPEN" status
4. Craftsmen can view and accept the job
5. Once accepted, job status changes to "IN_PROGRESS"
6. Client can track job progress
7. When complete, status changes to "COMPLETED"
8. Client can rate the craftsman (future feature)

### Craftsman Flow
1. Craftsman registers with craft specialty, experience, and portfolio
2. Craftsman browses open jobs
3. Craftsman accepts a job
4. Job status automatically changes to "IN_PROGRESS"
5. Craftsman completes the work
6. Craftsman updates job status to "COMPLETED"
7. Craftsman's completed project count increases
8. Rating may be updated based on client feedback (future feature)

## Job Status Lifecycle

```
OPEN → IN_PROGRESS → COMPLETED
  ↓
CANCELLED (can be set at any time)
```

- **OPEN**: Job is available for craftsmen to accept
- **IN_PROGRESS**: A craftsman has been assigned and is working on the job
- **COMPLETED**: Work is finished
- **CANCELLED**: Job was cancelled by client or craftsman

## Key Features

✅ **Dual User Roles**: Separate CLIENT and CRAFTSMAN roles with role-specific data
✅ **Job Lifecycle Management**: Track jobs from creation to completion
✅ **Assignment System**: Link craftsmen to jobs they're working on
✅ **Status Tracking**: Monitor job progress through different states
✅ **Client-Craftsman Matching**: Clients post jobs, craftsmen accept them
✅ **Profile Management**: Update user information and craftsman ratings
✅ **Backward Compatibility**: Legacy craftsmen table preserved for existing data

## Next Steps

To fully implement the user role system in your UI:

1. **Authentication Screen**: Create login/registration with role selection
2. **Client Dashboard**: Show created jobs and their status
3. **Craftsman Dashboard**: Show assigned jobs and available jobs
4. **Job Board**: Display open jobs for craftsmen to browse
5. **Job Details**: Show full job information with actions based on user role
6. **Profile Screens**: Different views for clients vs craftsmen
7. **Rating System**: Allow clients to rate craftsmen after job completion

## Migration Notes

The system maintains backward compatibility with the existing [`craftsmen`](convex-backend/convex/schema.ts:19) table. Jobs can reference either:
- `craftsmanId` (legacy, references craftsmen table)
- `assignedCraftsmanId` (new, references users table with CRAFTSMAN role)

When migrating existing data, you'll need to:
1. Create USER entries for existing craftsmen
2. Update jobs to use `clientId` and `assignedCraftsmanId`
3. Maintain the old `craftsmanId` field for reference during transition