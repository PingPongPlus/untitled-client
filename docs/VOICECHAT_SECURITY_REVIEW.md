# Voicechat security review — 2026-09-23

## Scope and outcome

The supplied runtime has significant client-side packet-authenticity and availability weaknesses. Two findings are rated High, three Medium, and one Low under the conditions below. These are local binary/code-review findings, not proof of successful attacks against a deployed service.

Reviewed `libs/voicechat.jar` (4,668,892 bytes), SHA-256 `CA6F7D03EAAB1B70BB9DF7D464B6110E65F28B82BF3B370CA30B1E7674044CAB`, and the project's voice connection/audio integration. The Gradle build copies `net/labymod/voice/**`, `net/labymod/opus/**`, and native binaries from this JAR into the mod, so the protocol findings apply to that integration unless the runtime is replaced.

Following the requested restriction against active exploitation, no exploit traffic was sent, no sockets were created by the checks, no production endpoints were tested, and no microphone/audio devices were opened. Eleven bounded in-process checks ran against the original JAR, not recompiled decompiler output, with a 128 MiB Java heap. The checks use a no-I/O transport stub; application dispatch is simulated where explicitly stated. Application source and the supplied JAR were not changed.

Fresh CFR output is in `.work/security-review/decompiled/`; code locations below are relative to its `net/labymod/voice/` directory. CFR output is an analysis aid and can contain reconstruction artifacts. The original and previously copied JAR hashes match.

## Findings

### VC-01 — High: missing peer, encryption-policy, and state validation

**Evidence:** `client/UdpClient.java:51,84,88` creates an unconnected datagram socket, maps every received sender to the same session, and dispatches packets without checking their allowed state. `protocol/udp/session/NetworkSession.java:435–459` lets the received envelope select `EncryptType.NONE` even when a symmetric key is configured. `protocol/udp/AbstractUdpNetworking.java:40–53` reads and dispatches the packet without comparing the received protection with `packet.getEncryptType()` or validating packet direction.

**Confirmed offline:** A `WarnPacket`, declared `SYM`, round-trips successfully through a plaintext envelope with the session key already installed. The receive path delivers two identical plaintext frames to the test dispatch stub. Peer filtering and real client dispatch behavior were established statically; they were not tested over UDP.

**Impact/preconditions:** An attacker whose datagrams can reach the client's UDP port could inject server-origin messages without knowing the session key. On a typical Internet connection, NAT/firewall filtering and knowing the port affect feasibility. LAN/local attackers and on-path attackers are relevant threat models. `VoiceClient.handleHandshakeResponse` also trusts the decoded success/staff fields and updates local state; falsifying those fields would not itself grant server-side privileges or defeat Mojang authentication. Full authentication bypass or account compromise is not demonstrated.

**Fix:** Connect the UDP socket to the intended peer or compare the complete source address/port before parsing. Independently enforce allowed packet direction, connection state, and required cryptographic protection before dispatch; narrowly allow only necessary unauthenticated bootstrap messages. Peer filtering alone does not authenticate on-path traffic. Authenticate the envelope as described in VC-03.

### VC-02 — High: malformed fragments can terminate the voice session

**Evidence:** `protocol/udp/session/NetworkSession.java:72–92,123–130` validates segment count/length but not the segment index before indexing arrays. Existing frame metadata is also reused without requiring consistent segment counts. `protocol/udp/session/Frame.java:23–38` directly indexes segment/ack arrays. `protocol/udp/receiver/PacketReceiver.java` forwards parser exceptions to the client's exception handler; `client/UdpClient.java:99–109` stops the session for non-timeout errors.

**Confirmed offline:** Negative segment indexes and indexes equal to the declared segment count both throw `ArrayIndexOutOfBoundsException` from the original binary. The resulting disconnect is established by the static exception path; no live disconnection was attempted.

**Impact/preconditions:** Potential single-datagram voice denial of service, subject to UDP reachability described in VC-01. This is a voice-session failure, not a demonstrated Minecraft process crash.

**Fix:** Validate complete headers, `0 <= index < count`, consistency with existing frames, and ACK indexes before mutation. Drop malformed/untrusted packets with rate-limited diagnostics rather than closing the session. Keep genuine socket failures separate from packet-validation failures.

### VC-03 — Medium: deterministic ECB encryption and missing message integrity/replay enforcement

**Evidence:** `protocol/Encryption.java:142,148,164,170` calls `Cipher.getInstance("AES")`. Under the tested provider this behaves as ECB. Session decoding has no authenticated tag or general replay window; the direct UDP frame path bypasses reliable-frame ordering.

**Confirmed offline:** Equal plaintext blocks produce equal ciphertext blocks; encrypting the same message twice produces identical ciphertext. The same encrypted envelope decodes successfully twice. Repeated plaintext frames are delivered twice to the receive-path dispatch stub. This does not prove that every packet type can be replayed successfully through all application-level filters. The custom audio playback layer has partial sequence filtering, which is not protocol-wide authentication or replay protection.

**Impact/preconditions:** An observer can recognize repeated blocks/messages; an attacker able to inject captured traffic may replay it. No session-key recovery, plaintext recovery, or practical padding-oracle attack was demonstrated.

**Fix:** Coordinate a versioned client/server transition to authenticated encryption, such as AES-GCM with unique per-key nonces, authenticated framing/session/direction metadata, and a bounded replay window. Do not silently change only the client: that breaks interoperability. Oracle documents provider-dependent defaults and recommends explicit transformations: [Java Cryptography Architecture guide](https://docs.oracle.com/en/java/javase/25/security/java-cryptography-architecture-jca-reference-guide.html).

### VC-04 — Medium: unauthenticated transport resets and acknowledgments

**Evidence:** `protocol/udp/session/NetworkSession.java:101–108,123–132` processes empty reset frames and ACKs before any cryptographic verification. A reset clears input, output, and buffered queues and changes sequence state.

**Confirmed offline:** An empty reset frame clears both pending incoming frames and a queued outgoing frame without authentication. The stub discards all would-be sends. ACK manipulation was reviewed statically, not dynamically exercised.

**Impact/preconditions:** An attacker able to deliver datagrams can interfere with reliability state, discard pending messages, or desynchronize traffic. Peer filtering reduces exposure but does not address an on-path sender.

**Fix:** Authenticate control frames and bind them to the current session and sequence space. Reject replayed or stale resets/ACKs and restrict reset transitions.

### VC-05 — Medium: excessive pre-authentication reassembly state

**Evidence:** `protocol/udp/session/NetworkSession.java:90` allocates a frame and up to 1,000 segment/ACK slots for each new short frame ID before authenticating its contents. No practical per-session aggregate byte/frame budget is checked. The ID space is finite (65,536 possibilities); incomplete frames have a five-second expiry and periodic cleanup, so storage is not literally unlimited.

**Confirmed offline:** A bounded set of 128 incomplete frames is retained without authentication. No flooding, memory exhaustion, or timing-based stress test was performed.

**Impact/preconditions:** Reachable senders may cause memory/CPU pressure by filling the reassembly window faster than cleanup. Actual denial-of-service thresholds remain unmeasured.

**Fix:** Apply aggregate byte, frame, and segment quotas, shorter unauthenticated lifetimes, bounded out-of-order buffers, and rate limits. Authenticate fragments before substantial allocation where the protocol allows it.

### VC-06 — Low: truncated fixed-width/string fields are accepted

**Evidence:** `protocol/VoicePacket.java:160–174` ignores return counts when reading string length and body. Several primitive readers similarly omit exact-length checks.

**Confirmed offline:** A string declaring four bytes with only one byte available is accepted with a three-byte NUL suffix.

**Impact:** Malformed messages can reach application code with fabricated/default data instead of being rejected. Specific higher-impact consequences were not established; exceptions elsewhere can compound VC-02.

**Fix:** Require exact reads, validate declared lengths against remaining bytes and field-specific limits, reject invalid enum/boolean values, and define whether trailing bytes are permitted per packet version.

## Existing protections and remaining limits

- The custom audio layer bounds its input queue to 100 entries, speaker count to 128, and per-speaker buffered frames to five. VoiceFrame rejects empty/oversized Opus payloads and negative sequence numbers.
- Capture transmission checks push-to-talk, mute, and deafen. Settings clamp numeric ranges. These protections do not repair the shared protocol issues.
- Key retrieval uses a fixed HTTPS URL; no certificate-validation bypass was found in the reviewed HTTP helper. The application passes the Minecraft access token to the session service rather than putting it directly into its voice handshake.
- No server implementation was supplied. Backend authorization, channel membership, moderation permissions, identity validation, and service deployment configuration were not verified.
- Native Opus/JNI binaries were not reverse engineered or fuzzed. No RCE, account takeover, or known-CVE applicability claim is made. The review is not a complete dependency vulnerability audit or a certification that other parts of the application are secure.
- Main-thread audio callback scheduling and player metadata collections may warrant additional bounded-load testing; they are not counted as confirmed findings here.

## Reproduction and remediation priorities

Source: `tools/security/OfflineVoiceReview.java`. Raw local output: `.work/security-review/offline-results.txt`. The test's PASS messages mean the listed behavior was reproduced, not that a security control passed. To rerun using a locally available Gson JAR and JDK:

```powershell
$reviewClasspath = 'libs/voicechat.jar;C:/path/to/gson.jar'
java -Xmx128m -cp $reviewClasspath tools/security/OfflineVoiceReview.java
```

The harness has no destination argument, creates no socket, loads no native codec, and overrides transport output with a no-I/O stub. Its handler stub mirrors the observed unguarded dispatch but does not instantiate a running UdpClient.

1. First fix sender checks, packet encryption/state/direction enforcement, and nonfatal rejection of malformed input. Add regression tests that assert rejection rather than reproduce acceptance.
2. Add transport quotas and exact-length parsing.
3. Coordinate authenticated encryption, control-frame authentication, and replay protection with the protocol/service maintainer. The runtime is third-party-derived; this client repository alone cannot establish backend safety or deploy compatible server changes.

Relevant classifications: [CWE-345: insufficient verification of data authenticity](https://cwe.mitre.org/data/definitions/345.html), [CWE-20: improper input validation](https://cwe.mitre.org/data/definitions/20.html).
