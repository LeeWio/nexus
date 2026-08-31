"use client";

import { ArrowDownIcon, ArrowUpIcon } from "@/components/icons";
import { HelloApple } from "@/components/home/hello-apple";
import { RepositoryActivity } from "@/components/home/repository-activity";
import {
  MotionCard,
  MotionChip,
  MotionItemCard,
  MotionKPI,
  MotionSurface,
  MotionTypography,
} from "@/components/ui";
import { MediaPlayButton } from "@/features/media/components/media-play-button";
import type { MediaItem } from "@/features/media/types";
import { useGetMarketIndexBySymbolQuery } from "@/lib/features/market";
import { useGetGitHubActivityQuery } from "@/lib/features/github";
import { useState, useMemo } from "react";
import { ItemCard, KPI, TrendChip } from "@heroui-pro/react";
import {
  Card,
  Chip,
  Skeleton,
  Typography,
  Button,
  Popover,
  TextArea,
  Description,
  Accordion,
} from "@heroui/react";
import { Icon } from "@iconify/react";
import { useMounted } from "@mantine/hooks";
import Image from "next/image";
import { motion, useReducedMotion } from "motion/react";

import GuestbookBoard from "@/components/corners/guestbook-board";
import { selectIsAuthenticated } from "@/lib/features/auth";
import { usePostGuestbookEntryMutation } from "@/lib/features/comment";
import { setLoginOpen } from "@/lib/features/ui";
import { useAppDispatch, useAppSelector } from "@/lib/hooks";
import { PencilToSquare, ChevronDown } from "@gravity-ui/icons";
import GradientText from "@/components/ui/gradient-text";
import { FeaturedWriting, MomentsShowcase } from "@/components/home/content-dispatch";

const MotionAccordion = motion.create(Accordion);

const easeOut = [0.22, 1, 0.36, 1] as const;

const mapSparkline = (data?: number[]) => {
  if (!data || data.length === 0) return [];
  const min = Math.min(...data);
  const max = Math.max(...data);
  const range = max - min;

  return data.map((value) => ({
    value: range === 0 ? 50 : ((value - min) / range) * 100,
  }));
};

const mockSong: MediaItem = {
  id: "1",
  title: "老歌",
  description: "安泊猜想",
  cover: "/IMG_5332.JPG",
  type: "track",
  tracks: [
    {
      id: "track-1",
      title: "老歌",
      artist: "安泊猜想",
      src: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
    },
  ],
};

const faqItems = [
  {
    content: "Stay informed about your account activity with real-time notifications.",
    iconUrl: "https://heroui-assets.nyc3.cdn.digitaloceanspaces.com/docs/3dicons/bell-small.png",
    subtitle: "Receive account activity updates",
    title: "Set Up Notifications",
  },
  {
    title: "Is This Website Finished?",
    subtitle: "Probably never — and that’s the point",
    content:
      "This site is an ongoing experiment. I’m constantly refining interactions, adding new ideas, and occasionally breaking things while trying something new.",
    iconUrl: "/icons/rocket.png",
  },
  {
    title: "How Fast Do You Reply?",
    subtitle: "Usually within a few days",
    content:
      "I read every message myself. Replies may take a little time, but I’ll usually get back to you within a few days.",
    iconUrl: "/icons/mail.png",
  },
  {
    title: "What Do You Like Building?",
    subtitle: "Design, technology, and everything in between",
    content:
      "I’m drawn to digital experiences where thoughtful design, technology, and storytelling come together — especially ideas that leave room for experimentation.",
    iconUrl: "https://img.icons8.com/3d-fluency/94/adobe-animate.png",
  },
  {
    content: "Enhance your browsing experience by installing our official browser extension",
    iconUrl: "https://heroui-assets.nyc3.cdn.digitaloceanspaces.com/docs/3dicons/compass-small.png",
    subtitle: "Connect your browser to your account",
    title: "Set up Browser Extension",
  },
  {
    content:
      "Begin your journey into the world of digital collectibles by creating your first NFT. ",
    iconUrl:
      "https://heroui-assets.nyc3.cdn.digitaloceanspaces.com/docs/3dicons/mint-collective-small.png",
    subtitle: "Create your first collectible",
    title: "Mint Collectible",
  },
];

export default function Home() {
  const mounted = useMounted();
  const shouldReduceMotion = useReducedMotion() ?? false;
  const { data: nasdaqData, isLoading: isNasdaqLoading } = useGetMarketIndexBySymbolQuery(
    { symbol: ".ixic", period: "1D" },
    { pollingInterval: 300000, refetchOnFocus: true }
  );
  const isPositive = nasdaqData ? nasdaqData.changePct >= 0 : false;
  const sparklineData = useMemo(() => {
    return mapSparkline(nasdaqData?.sparkline);
  }, [nasdaqData?.sparkline]);
  const { data: githubActivity, isLoading: isGitHubActivityLoading } = useGetGitHubActivityQuery(
    undefined,
    { pollingInterval: 3600000, refetchOnFocus: true }
  );

  const [isGuestbookPopoverOpen, setIsGuestbookPopoverOpen] = useState(false);

  const dispatch = useAppDispatch();
  const isAuthenticated = useAppSelector(selectIsAuthenticated);

  const reveal = (delay = 0, distance = 18) => ({
    initial: shouldReduceMotion ? false : { opacity: 0, y: distance },
    animate: { opacity: 1, y: 0 },
    transition: { duration: shouldReduceMotion ? 0 : 0.65, delay, ease: easeOut },
  });

  const revealInView = (delay = 0, distance = 20) => ({
    initial: shouldReduceMotion ? false : { opacity: 0, y: distance },
    whileInView: { opacity: 1, y: 0 },
    viewport: { once: true, amount: 0.3 },
    transition: { duration: shouldReduceMotion ? 0 : 0.65, delay, ease: easeOut },
  });

  return (
    <div className="bg-background w-full overflow-x-clip">
      <section
        aria-labelledby="home-hero-title"
        className="mx-auto flex min-h-[100dvh] w-full flex-col items-center justify-center px-6 pt-24 pb-16 text-center sm:px-10"
      >
        <MotionChip color="accent" size="sm" variant="soft" {...reveal(0.05, 10)}>
          A personal field journal
        </MotionChip>

        <div className="mt-1 w-full max-w-3xl" aria-hidden="true">
          <HelloApple />
        </div>

        <MotionTypography
          id="home-hero-title"
          type="h1"
          weight="bold"
          className="max-w-3xl text-[clamp(2.25rem,5vw,4.25rem)] leading-[0.98] tracking-[-0.055em]"
          {...reveal(0.18)}
        >
          A living notebook, kept in motion.
        </MotionTypography>

        <MotionTypography color="muted" type="body" className="mt-4 max-w-xl" {...reveal(0.26, 14)}>
          Software, markets, music, and the habits that shape the work.
        </MotionTypography>

        <MotionSurface
          variant="transparent"
          className="mt-9 flex max-w-lg flex-col items-center"
          {...reveal(0.34, 12)}
        >
          <Typography
            aria-hidden="true"
            className="font-mono tracking-[0.18em] uppercase"
            color="muted"
            type="body-xs"
          >
            Prologue · 01
          </Typography>
          <span className="bg-separator my-4 h-12 w-px" aria-hidden="true" />
          <Typography align="center" color="muted" type="body-sm" className="max-w-md italic">
            Begin with what is close at hand. A song still playing, a market moving, a thought not
            yet finished.
          </Typography>
          <motion.span
            aria-hidden="true"
            className="bg-foreground/55 mt-6 block size-1.5 rounded-full"
            animate={shouldReduceMotion ? undefined : { opacity: [0.28, 0.9, 0.28], y: [0, 5, 0] }}
            transition={{ duration: 2.4, ease: "easeInOut", repeat: Infinity }}
          />
        </MotionSurface>
      </section>

      <section
        id="lately"
        aria-labelledby="lately-title"
        className="mx-auto w-full max-w-6xl scroll-mt-24 px-6 py-24 sm:px-10 sm:py-32"
      >
        <header className="flex flex-col items-center text-center">
          <MotionChip size="sm" color="default" variant="secondary" {...revealInView(0, 10)}>
            Lately
          </MotionChip>
          <MotionTypography
            id="lately-title"
            align="center"
            type="h2"
            weight="bold"
            className="mt-4 text-[clamp(2rem,4vw,3.75rem)] tracking-[-0.04em]"
            {...revealInView(0.06)}
          >
            What I&apos;ve been up to
          </MotionTypography>
          <MotionTypography
            align="center"
            type="body"
            color="muted"
            className="mt-3"
            {...revealInView(0.12, 14)}
          >
            Listening, investing, building, and training.
          </MotionTypography>
        </header>

        <div className="mt-12 grid grid-cols-1 gap-5 lg:grid-cols-12 lg:gap-6">
          <MotionCard
            variant="secondary"
            className="group relative min-h-[34rem] overflow-hidden lg:col-span-7"
            {...revealInView(0.08, 28)}
          >
            <div className="absolute inset-0">
              <Image
                alt=""
                aria-hidden="true"
                className="scale-105 object-cover opacity-35 blur-2xl saturate-75"
                fill
                sizes="(min-width: 1024px) 55vw, 100vw"
                src="/IMG_5332.JPG"
              />
              <div className="from-background/10 via-surface-secondary/75 to-surface-secondary absolute inset-0 bg-linear-to-b" />
            </div>

            <Card.Header className="relative flex-row items-start justify-between">
              <Chip size="sm" variant="soft">
                Listening
              </Chip>
              <Typography
                aria-hidden="true"
                color="muted"
                type="body-xs"
                className="font-mono tracking-[0.14em]"
              >
                SIDE A · 01
              </Typography>
            </Card.Header>

            <Card.Content className="relative mt-auto gap-7">
              <motion.div
                className="relative aspect-square w-full max-w-64 overflow-hidden rounded-[1.75rem] shadow-2xl shadow-black/30 sm:max-w-72"
                whileInView={shouldReduceMotion ? undefined : { rotate: [-1.5, 0] }}
                viewport={{ once: true, amount: 0.5 }}
                transition={{ duration: 1.2, ease: easeOut }}
              >
                <Image
                  alt="Album cover for 老歌 by 安泊猜想"
                  className="object-cover"
                  fill
                  sizes="288px"
                  src="/IMG_5332.JPG"
                />
                {/* Subtle transparent boundary border layer */}
                <div className="pointer-events-none absolute inset-0 rounded-[1.75rem] border border-black/10 dark:border-white/10" />
              </motion.div>

              <div className="max-w-lg">
                <Typography type="h3" weight="semibold" className="tracking-[-0.03em]">
                  One song, kept close.
                </Typography>
                <Typography color="muted" type="body-sm" className="mt-2 leading-6">
                  Music that stayed after the rest of the queue moved on.
                </Typography>
              </div>
            </Card.Content>

            <Card.Footer className="relative mt-2">
              <MotionItemCard className="w-full" variant="transparent">
                <ItemCard.Content>
                  <ItemCard.Title>老歌</ItemCard.Title>
                  <ItemCard.Description>安泊猜想</ItemCard.Description>
                </ItemCard.Content>
                <ItemCard.Action>
                  <MediaPlayButton media={mockSong} shuffle size="sm" variant="tertiary" />
                </ItemCard.Action>
              </MotionItemCard>
            </Card.Footer>
          </MotionCard>

          <MotionCard
            variant="transparent"
            className="border-separator relative min-h-[34rem] border px-7 py-8 lg:col-span-5 lg:px-8"
            {...revealInView(0.14, 28)}
          >
            <Card.Header className="p-0">
              <Typography color="muted" type="body-xs" className="font-mono tracking-[0.14em]">
                FIELD SIGNAL · LIVE
              </Typography>
              <Card.Title className="mt-6 text-2xl tracking-[-0.03em]">
                Watching without rushing.
              </Card.Title>
              <Card.Description className="mt-2 max-w-sm leading-6">
                A small market signal, observed in context rather than isolation.
              </Card.Description>
            </Card.Header>

            <Card.Content className="mt-auto p-0 pt-12">
              {!mounted || (isNasdaqLoading && !nasdaqData) ? (
                <div
                  aria-label="Loading NASDAQ market data"
                  aria-live="polite"
                  className="grid min-h-28 grid-cols-2 items-end gap-6"
                >
                  <div className="flex flex-col gap-3">
                    <Skeleton className="h-4 w-20 rounded-md" />
                    <Skeleton className="h-9 w-28 rounded-lg" />
                    <Skeleton className="h-6 w-24 rounded-full" />
                  </div>
                  <Skeleton className="h-16 w-full rounded-xl" />
                </div>
              ) : nasdaqData ? (
                <MotionKPI variant="transparent">
                  <KPI.Header>
                    <Icon aria-hidden="true" icon="gravity-ui:target-dart" />
                    <KPI.Title>NASDAQ</KPI.Title>
                  </KPI.Header>
                  <KPI.Content className="grid-cols-[1fr_1fr] items-end">
                    <div className="flex flex-col gap-2">
                      <KPI.Value
                        className="text-foreground font-mono text-3xl leading-none font-black tabular-nums"
                        maximumFractionDigits={2}
                        value={nasdaqData.current}
                      />
                      <TrendChip trend={isPositive ? "up" : "down"} variant="tertiary">
                        <TrendChip.Indicator>
                          {isPositive ? <ArrowUpIcon /> : <ArrowDownIcon />}
                        </TrendChip.Indicator>
                        <span className="font-mono font-semibold tabular-nums">
                          {Math.abs(nasdaqData.changePct).toFixed(2)}%
                        </span>
                        <TrendChip.Suffix>today</TrendChip.Suffix>
                      </TrendChip>
                    </div>
                    <KPI.Chart
                      color="var(--color-accent)"
                      data={sparklineData}
                      height={60}
                      strokeWidth={1.5}
                    />
                  </KPI.Content>
                </MotionKPI>
              ) : (
                <div className="flex min-h-28 items-center gap-3" role="status">
                  <span className="bg-default-100 text-muted flex size-10 shrink-0 items-center justify-center rounded-full">
                    <Icon aria-hidden="true" icon="gravity-ui:chart-line" />
                  </span>
                  <div>
                    <Typography type="body-sm" weight="semibold">
                      Market signal unavailable
                    </Typography>
                    <Typography color="muted" type="body-xs">
                      The latest NASDAQ reading could not be loaded.
                    </Typography>
                  </div>
                </div>
              )}
            </Card.Content>
          </MotionCard>

          <MotionCard
            variant="tertiary"
            className="relative min-h-[30rem] overflow-hidden lg:col-span-12 lg:grid lg:grid-cols-[0.8fr_1.2fr] lg:items-center lg:gap-12 lg:p-12"
            {...revealInView(0.18, 24)}
          >
            <span
              aria-hidden="true"
              className="bg-accent/8 absolute -top-20 -right-16 size-72 rounded-full blur-3xl"
            />

            {/* Left side: Editorial introduction */}
            <div className="relative flex flex-col gap-5 p-6 lg:p-0">
              <div>
                <Typography
                  type="body-xs"
                  color="muted"
                  className="font-mono tracking-[0.14em] uppercase"
                >
                  Building & training
                </Typography>
                <Card.Title className="mt-4 text-3xl font-bold tracking-[-0.03em]">
                  Practice is part of the archive.
                </Card.Title>
                <Card.Description className="mt-2 max-w-sm text-sm leading-relaxed">
                  Real-time indicators documenting personal progress, deep focus blocks, and active
                  code commitments.
                </Card.Description>
              </div>
              <span className="bg-default-100/50 my-1 h-px w-24" />
              <Typography
                type="body-sm"
                color="muted"
                className="max-w-md text-sm leading-relaxed italic"
              >
                “The unfinished work matters: systems shipped, miles logged, and questions carried
                forward.”
              </Typography>
            </div>

            {/* Right side: Live Telemetry KPIs */}
            <div className="relative grid gap-4 p-6 sm:grid-cols-3 lg:p-0">
              {/* KPI 1: Deep Work focus */}
              <KPI className="bg-background/40 rounded-2xl p-5 shadow-sm">
                <KPI.Header>
                  <KPI.Title className="text-muted/60 font-mono text-[10px] font-bold tracking-wider uppercase">
                    Today&apos;s Focus
                  </KPI.Title>
                </KPI.Header>
                <KPI.Content className="mt-2.5 items-end gap-1">
                  <KPI.Value
                    className="text-foreground font-mono text-2xl leading-none font-black tabular-nums"
                    value={4.8}
                    style="decimal"
                    maximumFractionDigits={1}
                  />
                  <div className="text-muted/60 mt-1 font-mono text-[10px] font-medium">hours</div>
                </KPI.Content>
                <KPI.Progress className="mt-4" value={90} status="success" />
              </KPI>

              {/* KPI 2: Written Chronicles size */}
              <KPI className="bg-background/40 rounded-2xl p-5 shadow-sm">
                <KPI.Header>
                  <KPI.Title className="text-muted/60 font-mono text-[10px] font-bold tracking-wider uppercase">
                    Written Essays
                  </KPI.Title>
                </KPI.Header>
                <KPI.Content className="mt-2.5 items-end gap-1">
                  <KPI.Value
                    className="text-foreground font-mono text-2xl leading-none font-black tabular-nums"
                    value={18.4}
                    style="decimal"
                    maximumFractionDigits={1}
                  />
                  <div className="text-muted/60 mt-1 font-mono text-[10px] font-medium">
                    k words
                  </div>
                </KPI.Content>
                <KPI.Progress className="mt-4" value={85} status="success" />
              </KPI>

              {/* KPI 3: Repository Commits */}
              <KPI className="bg-background/40 rounded-2xl p-5 shadow-sm">
                <KPI.Header>
                  <KPI.Title className="text-muted/60 font-mono text-[10px] font-bold tracking-wider uppercase">
                    Active Commits
                  </KPI.Title>
                </KPI.Header>
                <KPI.Content className="mt-2.5 items-end">
                  <div className="flex w-full flex-col gap-1">
                    <KPI.Value
                      className="text-foreground font-mono text-2xl leading-none font-black tabular-nums"
                      value={452}
                      style="decimal"
                      maximumFractionDigits={0}
                    />
                    <div className="mt-1 flex items-center gap-1">
                      <TrendChip trend="up" variant="tertiary" className="px-1 py-0.5 text-[10px]">
                        12%
                        <TrendChip.Suffix className="ml-0.5 text-[8px]">MoM</TrendChip.Suffix>
                      </TrendChip>
                    </div>
                  </div>
                </KPI.Content>
                <KPI.Progress className="mt-4.5" value={95} status="success" />
              </KPI>
            </div>
          </MotionCard>
        </div>

        <motion.section
          aria-labelledby="github-activity-title"
          className="mt-20 grid w-full items-start gap-12 lg:grid-cols-[minmax(0,0.72fr)_minmax(0,1.28fr)] lg:gap-16"
          {...revealInView(0.2, 20)}
        >
          <div className="max-w-md lg:sticky lg:top-28 lg:pt-1">
            <Typography
              color="muted"
              type="body-xs"
              className="font-mono tracking-[0.14em] uppercase"
            >
              Development log
            </Typography>
            <Typography
              id="github-activity-title"
              type="h2"
              weight="bold"
              className="mt-4 text-[clamp(2rem,4vw,3.5rem)] leading-[1.02] tracking-[-0.045em]"
            >
              What&apos;s Being Built
            </Typography>
            <Typography color="muted" type="body" className="mt-5 max-w-sm leading-relaxed italic">
              A closer look at the work behind the site: new features, fixes, experiments, and
              everything slowly taking shape.
            </Typography>
          </div>

          <div className="flex w-full min-w-0 lg:justify-end">
            {!mounted || (isGitHubActivityLoading && !githubActivity) ? (
              <div
                aria-label="Loading GitHub repository activity"
                aria-live="polite"
                className="w-full max-w-[620px] min-w-0"
              >
                <Skeleton className="mb-6 h-5 w-32 rounded-md" />
                <div className="grid gap-5 pl-9">
                  <Skeleton className="h-20 w-full rounded-xl" />
                  <Skeleton className="h-32 w-full rounded-xl" />
                  <Skeleton className="h-24 w-full rounded-xl" />
                </div>
              </div>
            ) : githubActivity?.available ? (
              <RepositoryActivity activity={githubActivity} />
            ) : (
              <Card className="w-full max-w-[620px] min-w-0 p-5" variant="secondary">
                <Card.Title className="text-sm">GitHub activity is unavailable</Card.Title>
                <Card.Description className="mt-1 text-xs">
                  The public activity feed could not be loaded right now.
                </Card.Description>
              </Card>
            )}
          </div>
        </motion.section>
      </section>

      <FeaturedWriting />
      <MomentsShowcase />

      <section
        id="guestbook"
        aria-labelledby="guestbook-title"
        className="mx-auto w-full scroll-mt-24 py-24 sm:py-32"
      >
        <header className="relative mx-auto flex flex-col items-center px-6 text-center sm:px-10">
          <Popover isOpen={isGuestbookPopoverOpen} onOpenChange={setIsGuestbookPopoverOpen}>
            <Popover.Trigger className="absolute -top-8 -right-20">
              <Image
                alt="Guestbook decorative animation"
                aria-hidden="true"
                height={112}
                src="/Animation.svg"
                unoptimized
                width={112}
              />
            </Popover.Trigger>
            <Popover.Content
              className="border-default-200/50 bg-surface/90 w-80 border shadow-xl backdrop-blur-md"
              placement="bottom end"
            >
              <Popover.Dialog className="p-4 outline-none">
                <Popover.Arrow />
                <GuestbookQuickForm onClose={() => setIsGuestbookPopoverOpen(false)} />
              </Popover.Dialog>
            </Popover.Content>
          </Popover>

          <MotionChip size="sm" color="accent" variant="soft" {...revealInView(0, 10)}>
            Guestbook
          </MotionChip>

          <MotionTypography
            id="guestbook-title"
            align="center"
            type="h2"
            weight="bold"
            className="mt-4 text-center text-[clamp(2.25rem,4.5vw,3.75rem)] leading-[1.08] tracking-[-0.04em] text-balance"
            {...revealInView(0.06)}
          >
            <GradientText className="pointer-events-none cursor-default !rounded-none bg-transparent !p-0 shadow-none backdrop-blur-none ![font:inherit]">
              Since you’re here,
            </GradientText>
          </MotionTypography>

          <MotionTypography
            align="center"
            type="h3"
            className="text-center text-[clamp(2.25rem,4.5vw,3.75rem)] leading-[1.08] tracking-[-0.04em] text-balance"
            {...revealInView(0.06)}
          >
            <GradientText className="pointer-events-none cursor-default !rounded-none bg-transparent !p-0 shadow-none backdrop-blur-none ![font:inherit]">
              tell me what’s on your mind.
            </GradientText>
          </MotionTypography>
        </header>

        {/* Beautiful Guestbook Testimonials Board */}
        <motion.div className="mt-6 w-full" {...revealInView(0.12, 20)}>
          <GuestbookBoard />
        </motion.div>

        {/* Constrained Comments & Feedback Content */}
        <div className="mx-auto mt-16 flex w-full max-w-3xl flex-col gap-10">
          {mounted && !isAuthenticated ? (
            <motion.div className="w-full" {...revealInView(0.2, 16)}>
              <div className="bg-surface-secondary/30 flex flex-col gap-4 rounded-2xl p-5 shadow-sm sm:flex-row sm:items-center sm:justify-between sm:p-6">
                <div>
                  <Typography type="body-sm" weight="semibold">
                    Sign in to add an entry
                  </Typography>
                  <Typography color="muted" type="body-xs" className="mt-1">
                    Reading is open to everyone. Sign in to leave a note or reply.
                  </Typography>
                </div>
                <Button
                  size="sm"
                  variant="secondary"
                  className="shrink-0 font-medium"
                  onPress={() => dispatch(setLoginOpen(true))}
                >
                  <PencilToSquare aria-hidden="true" className="size-4" />
                  Sign in to write
                </Button>
              </div>
            </motion.div>
          ) : null}
        </div>
      </section>

      <section
        id="faq"
        aria-labelledby="faq-title"
        className="mx-auto flex w-full max-w-4xl scroll-mt-24 flex-col items-center px-6 py-24 text-center sm:px-10 sm:py-32"
      >
        <header className="flex flex-col items-center text-center">
          <MotionChip size="sm" color="default" variant="secondary" {...revealInView(0, 10)}>
            FAQ
          </MotionChip>
          <MotionTypography
            id="faq-title"
            align="center"
            type="h2"
            weight="bold"
            className="mt-4 text-[clamp(2rem,4vw,3.75rem)] tracking-[-0.04em]"
            {...revealInView(0.06)}
          >
            Questions, answered.
          </MotionTypography>
          <MotionTypography
            align="center"
            type="body"
            color="muted"
            className="mt-3 max-w-xl text-balance"
            {...revealInView(0.12, 14)}
          >
            Some answers to questions that tend to come up.
          </MotionTypography>
        </header>

        <MotionAccordion
          className="bg-surface-1/10 mt-12 w-full rounded-2xl"
          variant="surface"
          {...revealInView(0.18, 20)}
        >
          {faqItems.map((item, index) => (
            <Accordion.Item
              key={index}
              className="group/item first:**:data-[slot=accordion-trigger]:rounded-t-2xl last:[&:not(:has([data-slot=accordion-trigger][aria-expanded='true']))_[data-slot=accordion-trigger]]:rounded-b-2xl"
            >
              <Accordion.Heading>
                <Accordion.Trigger className="group hover:bg-surface flex items-center gap-2 transition-none">
                  {item.iconUrl ? (
                    <Image
                      alt={item.title}
                      className="h-11 w-11 transition-[scale,rotate] duration-300 ease-out group-hover/item:scale-120 group-hover/item:-rotate-10 group-hover/item:drop-shadow-lg"
                      src={item.iconUrl}
                      width={44}
                      height={44}
                    />
                  ) : null}
                  <div className="flex flex-col gap-0 text-start">
                    <span className="leading-5 font-medium">{item.title}</span>
                    <span className="text-muted/80 leading-6 font-normal">{item.subtitle}</span>
                  </div>
                  <Accordion.Indicator className="text-muted/50 [&>svg]:size-4">
                    <ChevronDown />
                  </Accordion.Indicator>
                </Accordion.Trigger>
              </Accordion.Heading>
              <Accordion.Panel>
                <Accordion.Body className="text-muted/80 text-start">{item.content}</Accordion.Body>
              </Accordion.Panel>
            </Accordion.Item>
          ))}
        </MotionAccordion>
      </section>
    </div>
  );
}

interface GuestbookQuickFormProps {
  onClose: () => void;
}

function GuestbookQuickForm({ onClose }: GuestbookQuickFormProps) {
  const dispatch = useAppDispatch();
  const isAuthenticated = useAppSelector(selectIsAuthenticated);
  const [content, setContent] = useState("");
  const [postEntry, { isLoading }] = usePostGuestbookEntryMutation();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!content.trim() || isLoading) return;
    try {
      await postEntry({ content: content.trim() }).unwrap();
      setContent("");
      onClose();
    } catch (err) {
      console.error("Failed to post entry:", err);
    }
  };

  if (!isAuthenticated) {
    return (
      <div className="flex flex-col gap-3 text-start">
        <div className="flex items-center gap-2">
          <span className="text-accent text-base">✨</span>
          <Popover.Heading className="text-foreground text-sm font-semibold tracking-tight">
            Sign the Guestbook
          </Popover.Heading>
        </div>
        <p className="text-muted/80 text-[11px] leading-relaxed">
          Leave a message on our wall to mark your visit. Reading is open to everyone, but writing
          requires a quick sign-in.
        </p>
        <div className="mt-1 flex items-center justify-between gap-3">
          <Button
            size="sm"
            variant="primary"
            className="bg-accent h-8 px-4 text-xs font-semibold text-white hover:brightness-105"
            onPress={() => {
              onClose();
              dispatch(setLoginOpen(true));
            }}
          >
            <Icon icon="lucide:pencil-line" className="size-3.5" />
            Sign in to write
          </Button>
          <Button
            size="sm"
            variant="ghost"
            className="text-muted/80 h-8 px-3 text-xs font-medium"
            onPress={onClose}
          >
            Close
          </Button>
        </div>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-3 text-start">
      <div className="flex items-center gap-2">
        <span className="text-accent text-base">✨</span>
        <Popover.Heading className="text-foreground text-sm font-semibold tracking-tight">
          Before You Go
        </Popover.Heading>
      </div>

      <div className="flex w-full flex-col gap-2">
        <TextArea
          aria-label="Guestbook message"
          placeholder="Write something for the next explorer..."
          rows={3}
          maxLength={280}
          value={content}
          onChange={(e) => setContent(e.target.value)}
          disabled={isLoading}
        />
        <Description id="textarea-controlled-description">
          Characters: {content.length} / 280
        </Description>
      </div>

      <div className="flex items-center justify-between gap-4">
        <Button size="sm" fullWidth variant="ghost" onPress={onClose} isDisabled={isLoading}>
          Cancel
        </Button>
        <Button
          type="submit"
          fullWidth
          size="sm"
          variant="primary"
          isDisabled={!content.trim() || isLoading}
        >
          {isLoading ? (
            <>
              <Icon icon="lucide:loader-2" className="size-3.5 animate-spin" />
              Posting...
            </>
          ) : (
            <>
              <Icon icon="lucide:send" className="size-3.5" />
              Submit
            </>
          )}
        </Button>
      </div>
    </form>
  );
}
