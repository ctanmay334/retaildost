---
name: RetailDost
colors:
  surface: '#fbf8ff'
  surface-dim: '#dbd9e1'
  surface-bright: '#fbf8ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f5f2fb'
  surface-container: '#efedf5'
  surface-container-high: '#e9e7f0'
  surface-container-highest: '#e4e1ea'
  on-surface: '#1b1b21'
  on-surface-variant: '#454652'
  inverse-surface: '#303036'
  inverse-on-surface: '#f2eff8'
  outline: '#767683'
  outline-variant: '#c6c5d4'
  surface-tint: '#4955b3'
  primary: '#0b1a7d'
  on-primary: '#ffffff'
  primary-container: '#283593'
  on-primary-container: '#9aa5ff'
  inverse-primary: '#bcc2ff'
  secondary: '#006b5e'
  on-secondary: '#ffffff'
  secondary-container: '#94f0df'
  on-secondary-container: '#006f62'
  tertiary: '#4c1a00'
  on-tertiary: '#ffffff'
  tertiary-container: '#702a00'
  on-tertiary-container: '#f79261'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dfe0ff'
  primary-fixed-dim: '#bcc2ff'
  on-primary-fixed: '#000c62'
  on-primary-fixed-variant: '#303c9a'
  secondary-fixed: '#97f3e2'
  secondary-fixed-dim: '#7ad7c6'
  on-secondary-fixed: '#00201b'
  on-secondary-fixed-variant: '#005047'
  tertiary-fixed: '#ffdbcc'
  tertiary-fixed-dim: '#ffb694'
  on-tertiary-fixed: '#351000'
  on-tertiary-fixed-variant: '#793105'
  background: '#fbf8ff'
  on-background: '#1b1b21'
  surface-variant: '#e4e1ea'
typography:
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
  headline-sm:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 26px
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.05em
  label-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  baseline: 8px
  margin-mobile: 16px
  gutter-mobile: 12px
  touch-target-min: 56px
  stack-sm: 8px
  stack-md: 16px
  stack-lg: 24px
---

## Brand & Style
The design system is engineered for the high-utility environment of Indian retail commerce. It prioritizes reliability, speed, and absolute clarity. The target audience—RetailDost store owners—requires a UI that remains legible under harsh shop lighting and remains performant on entry-level Android hardware.

The design style is **Corporate / Modern** with a heavy emphasis on **High-Contrast** utility. It follows Material 3 logic but strips away unnecessary ornamentation to focus on core merchant intents: billing, inventory tracking, and credit management. The emotional response should be one of professional empowerment and trust, ensuring the store owner feels their digital ledger is as sturdy as their physical shop.

## Colors
This design system utilizes a high-chroma palette to ensure distinct visual hierarchy even at lower screen brightness. 

- **Primary (Deep Indigo):** Used for primary actions, headers, and branding to evoke stability.
- **Secondary (Teal):** Dedicated to commerce-related flows, such as "Add to Cart" or "Check Out."
- **Status Colors:** Green, Amber, and Crimson are used with high saturation to signal stock levels and payment statuses immediately.
- **Surface:** A neutral, cool-gray and white foundation minimizes glare and maximizes text contrast (meeting WCAG AAA standards where possible).

## Typography
Inter is selected for its exceptional legibility and robust support for Devanagari scripts, ensuring Hindi translations are as readable as English. 

The type scale is intentionally oversized. Headlines are bold and tight to allow for quick scanning of product names and prices. Labels use increased letter-spacing and semi-bold weights to remain distinct at small sizes. All numerical data (prices, stock counts) should use `headline-md` or `headline-sm` to prevent errors during rapid transactions.

## Layout & Spacing
The layout follows a **Fluid Grid** model optimized for the vertical orientation of mobile devices. 

- **Grid:** A 4-column system for mobile, expanding to 8 columns for tablets. 
- **Rhythm:** An 8px baseline grid governs all vertical spacing.
- **Touch Targets:** A strict minimum of 56px for all interactive elements to accommodate fast-paced usage and varying levels of digital literacy.
- **Safe Zones:** Generous bottom margins (32px+) are maintained to prevent accidental triggers of Android system navigation gestures.

## Elevation & Depth
Depth is conveyed through **Tonal Layers** and subtle **Ambient Shadows**. 

1.  **Level 0 (Background):** Flat `#f5f5f5` for the main canvas.
2.  **Level 1 (Cards):** White surfaces with a 1px `#e0e0e0` stroke and no shadow for high-performance rendering.
3.  **Level 2 (Floating/Active):** Primary buttons and FABs use a soft, 15% opacity Indigo shadow to indicate interactability.
4.  **Level 3 (Modals):** Full-screen or bottom-sheet overlays with a dimming backdrop (40% black) to focus attention.

## Shapes
The shape language uses **Soft** geometry (4px - 12px corners). This provides a modern feel without the "childish" appearance of fully rounded pill shapes, maintaining a professional tool-like aesthetic. 

- **Small Components (Checkboxes, Inputs):** 4px radius.
- **Medium Components (Cards, Buttons):** 8px radius.
- **Large Components (Bottom Sheets):** 12px top-only radius.

## Components
- **Buttons:** Primary buttons must be 56px high, using the Primary Deep Indigo background with white text. Secondary buttons use the Teal palette.
- **FAB (Floating Action Button):** A circular 64px button anchored to the bottom right. Primarily used for "Quick Scan" or "Voice Input."
- **Input Fields:** Outlined style with a 2px stroke when focused. Labels must always be visible (never use placeholder-only inputs) to assist with cognitive load.
- **Status Badges:** Small, high-contrast chips (e.g., "Low Stock" in Amber) placed at the top right of product cards.
- **Lists:** Swipe-to-action list items for deleting or editing ledger entries. Items must have a minimum height of 72px to ensure readability of multi-line text (Product Name + SKU).
- **Inventory Cards:** Structured with the image on the left, primary text in the center, and a large "Add/Edit" stepper on the right for immediate quantity adjustments.