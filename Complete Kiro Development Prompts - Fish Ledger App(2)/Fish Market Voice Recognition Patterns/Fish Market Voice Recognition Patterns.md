\# Fish Market Voice Recognition Patterns for Ghana ## Core Transaction Flow Patterns

\### \*\*Pattern 1: Customer Inquiry → Price → Negotiation → Sale\*\*

\#### \*\*Phase 1: Customer Interest/Inquiry\*\* ```

English Patterns:

- "How much be this fish?"
- "What's the price of the tilapia?"
- "How much you dey sell the tuna?"
- "This mackerel, how much?"
- "Give me price for this one"

Twi Patterns:

- "Sɛn na ɛyɛ?" (How much is it?)
- "Nam yi yɛ sɛn?" (How much is this fish?)
- "Ɛbo yɛ sɛn?" (What's the price?)

Mixed Code-Switching:

- "This fish yɛ sɛn?"
- "How much be this nam?"
- "Price yɛ sɛn for this tilapia?" ```

\*\*AI Recognition Triggers\*\*:

- Question words: "How much", "What's", "Sɛn na"
- Fish references: "fish", "nam", specific fish names
- Price indicators: "price", "cost", "ɛbo"

\#### \*\*Phase 2: Seller Price Response\*\* ```

English Patterns:

- "This one be 15 cedis"
- "I dey sell am 20 Ghana cedis"
- "The price be 12 cedis"
- "Make you give me 18 cedis"
- "This big one cost 25"

Twi Patterns:

- "Ɛyɛ cedis dunum" (It's 10 cedis)
- "Sika yi yɛ cedis aduonum" (This money is 20 cedis)
- "Ma me cedis dubaako" (Give me 11 cedis)

Mixed Patterns:

- "Ɛyɛ 15 cedis"
- "This one cost cedis aduonum"
- "Price be cedis dubaako"

\```

\*\*AI Recognition Triggers\*\*:

- Numbers: "15", "twenty", "dunum" (10), "aduonum" (20)
- Currency: "cedis", "Ghana cedis", "sika"
- Price indicators: "cost", "be", "ɛyɛ"

\#### \*\*Phase 3: Negotiation\*\* ```

Customer Bargaining:

- "Can you reduce small?"
- "Make you do 10 cedis for me"
- "Too much o, reduce am"
- "Ɛyɛ dɔɔso" (It's too much)
- "Reduce am make we agree"

Seller Counter-offers:

- "I fit do 12 cedis last price"
- "Make you add small, 14 cedis"
- "Last last, give me 13"
- "No wahala, 11 cedis"
- "Sɛ wopɛ a, fa cedis dubaako" (If you want, take 11 cedis) ```

\*\*AI Recognition Triggers\*\*:

- Negotiation phrases: "reduce", "last price", "add small", "too much"
- Agreement indicators: "okay", "fine", "no wahala", "sɛ wopɛ a"

\#### \*\*Phase 4: Final Agreement & Payment\*\* ```

Agreement Confirmation:

- "Okay, I take am for 12 cedis"
- "Fine, give me the fish"
- "I agree for the 13 cedis"
- "Ɛyɛ, megye" (Okay, I'll take it)

Payment Exchange:

- "Here be your money"
- "Take your 12 cedis"
- "This be the money"
- "Sika ni" (Here's the money)

Seller Confirmation:

- "Thank you, your fish ready"
- "Take your fish"
- "Medaase" (Thank you)
- "Go well"

\```

\*\*AI Recognition Triggers\*\*:

- Final confirmation: "okay", "fine", "I take", "megye"
- Payment phrases: "here be", "take your", "sika ni"
- Transaction completion: "thank you", "medaase", "ready"

\## Fish-Specific Product Recognition

\### \*\*Common Fish Types in Ghana Markets\*\*

\```

Tilapia Variations:

- "tilapia", "tuo", "apateshi"
- Size references: "big tilapia", "small tuo", "medium apateshi"

Tuna Variations:

- "tuna", "light meat", "koobi" (smoked)
- "fresh tuna", "tuna steak"

Mackerel Variations:

- "mackerel", "kpanla", "titus"
- "big mackerel", "kpanla kakra" (small mackerel)

Sardines/Herrings:

- "sardine", "herring", "anchovies"
- "one tin sardine", "small herrings"

Local Fish:

- "red fish", "snapper", "croaker"
- "sea bass", "grouper"

\```

\### \*\*Quantity Recognition Patterns\*\*

\```

Counting Patterns:

- "One fish", "Two pieces", "Three tilapia"
- "Baako" (one), "Mmienu" (two), "Mmiɛnsa" (three)
- "One big one", "Two small ones"

Weight/Size Patterns:

- "This big one", "Small small fish"
- "Heavy one", "The fat tilapia"
- "Kɛseɛ" (big), "ketewa" (small)

Bundle/Package Patterns:

- "One bag", "This basket"
- "All these ones", "The whole thing"
- "Bundle for 20 cedis"

\```

\## Advanced Recognition Patterns

\### \*\*Context-Aware Transaction Triggers\*\*

\#### \*\*Transaction Start Indicators\*\*

\```

Strong Signals (High Confidence):

- Customer approaches + fish name + price question
- "How much" + fish type
- Pointing gestures (if video enabled) + "this one"

Medium Signals:

- Fish name mentioned + numbers
- "Give me" + fish reference
- Currency mentioned in context

Weak Signals (Need additional context):

- Just numbers mentioned
- General conversation about fish
- Background chatter

\```

\#### \*\*Transaction End Indicators\*\* ```

Strong Completion Signals:

- Payment phrases + thank you
- "Take your fish" + "medaase"
- Money amount + "here be your money"

Medium Completion Signals:

- Agreement + fish preparation sounds
- "Okay" + "wrap am for me"
- Number confirmation + closing phrases

False Positive Filters:

- Conversations about weather/family
- Discussions with other sellers
- Phone conversations

\```

\### \*\*Speaker Identification Patterns\*\*

\#### \*\*Seller Voice Characteristics\*\* ```

Typical Seller Phrases:

- "Fresh fish here!"
- "Come and buy!"
- "What you want?"
- "I get plenty fish today"
- Pricing announcements

Seller Speech Patterns:

- More authoritative tone
- Familiar with fish names/prices
- Speaks more frequently
- Uses market-specific vocabulary ```

\#### \*\*Customer Voice Characteristics\*\* ```

Typical Customer Phrases:

- Questions about price/quality
- Negotiation attempts
- Comparison shopping
- "Let me see this one"

Customer Speech Patterns:

- More questioning tone
- Less familiar with exact fish terminology
- Speaks less frequently in interaction
- Uses more general language

\```

\## Technical Implementation Strategy

\### \*\*AI Training Data Requirements\*\*

\#### \*\*Audio Data Collection\*\*

\```

Target Recordings:

- 500+ complete fish transactions
- 10+ different markets across Ghana
- 50+ different seller voices
- Various times of day/market conditions
- Different phone positions/distances

\```

\#### \*\*Annotation Requirements\*\*

\```

Label Categories:

- Speaker ID (seller vs customer)
- Transaction phase (inquiry/price/negotiation/completion)
- Fish type mentioned
- Price/quantity mentioned
- Confidence level (clear/unclear/background)

\```

\### \*\*Recognition Confidence Scoring\*\*

\#### \*\*High Confidence Transactions (Auto-record)\*\* ```

Criteria:

- Clear fish name + clear price + payment confirmation
- Seller voice + customer voice identified
- Complete transaction flow detected
- Price range reasonable for fish type

Example: "Tilapia" + "15 cedis" + "Here be your money" + "Thank you" ```

\#### \*\*Medium Confidence (Flag for Review)\*\* ```

Criteria:

- Some transaction elements missing
- Unclear audio quality
- Unusual price ranges
- Mixed conversation topics

Example: Background conversation + fish mention + unclear price ```

\#### \*\*Low Confidence (Ignore)\*\* ```

Criteria:

- No clear transaction flow
- Only background chatter
- Phone conversations
- Non-business discussions

\```

\### \*\*Error Correction & Learning System\*\*

\#### \*\*Seller Feedback Mechanism\*\*

\```

Daily Review Process:

- End-of-day summary: "I recorded 12 fish sales today"
- Quick correction: "Actually, it was 10 sales"
- Specific corrections: "That 20 cedis was for two fish, not one"

Voice Correction Commands:

- "That was wrong" - marks last transaction for review
- "Correct that" - opens correction interface
- "Good job" - confirms accuracy

\```

\#### \*\*Continuous Learning\*\*

\```

Pattern Recognition Improvement:

- Learn seller's specific vocabulary over time
- Adapt to customer accent patterns in that market
- Improve fish type recognition for local varieties
- Adjust price range expectations seasonally

\```

\## Implementation Priorities

\### \*\*Phase 1: Core Recognition (MVP)\*\*

1. \*\*Basic Transaction Flow\*\*: Inquiry → Price → Payment
1. \*\*Top 5 Fish Types\*\*: Tilapia, Tuna, Mackerel, Sardine, Red Fish
1. \*\*Clear Audio Only\*\*: Skip unclear/noisy recordings
1. \*\*English + Basic Twi\*\*: Core language patterns
1. \*\*Simple Quantities\*\*: One, two, three pieces

\### \*\*Phase 2: Enhanced Recognition\*\*

1. \*\*Negotiation Patterns\*\*: Bargaining detection
1. \*\*More Fish Varieties\*\*: 15+ common types
1. \*\*Complex Quantities\*\*: Bundles, weights, sizes
1. \*\*Multi-language\*\*: Ga, Ewe additions
1. \*\*Context Awareness\*\*: Market time, weather factors

\### \*\*Phase 3: Advanced Intelligence\*\*

1. \*\*Customer Recognition\*\*: Repeat buyer identification
1. \*\*Seasonal Patterns\*\*: Price/demand variations
1. \*\*Inventory Correlation\*\*: Sales vs stock tracking
1. \*\*Predictive Insights\*\*: Best selling times/products

The key to success is starting simple with the most common, clearest patterns and gradually building complexity as the AI learns from real usage data.
