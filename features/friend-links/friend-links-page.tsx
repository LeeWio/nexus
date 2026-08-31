"use client";

import { ArrowRotateLeft, ArrowUpRight, CircleLink, Globe, PaperPlane } from "@gravity-ui/icons";
import { EmptyState } from "@heroui-pro/react";
import {
  Avatar,
  Button,
  Card,
  Chip,
  FieldError,
  Form,
  Input,
  Label,
  SearchField,
  Skeleton,
  TextArea,
  TextField,
  Typography,
  toast,
} from "@heroui/react";
import { useDeferredValue, useState, type FormEvent } from "react";

import {
  type FriendLinkRequest,
  type FriendLinkResponse,
  useApplyFriendLinkMutation,
  useGetPublicFriendLinksQuery,
} from "@/lib/features/friend-link";

type FriendLinkApplication = Required<
  Pick<FriendLinkRequest, "name" | "url" | "email" | "avatar" | "description">
>;

const EMPTY_APPLICATION: FriendLinkApplication = {
  name: "",
  url: "",
  avatar: "",
  description: "",
  email: "",
};

function toSafeExternalUrl(value?: string | null) {
  if (!value?.trim()) return undefined;

  try {
    const url = new URL(value.trim());
    return url.protocol === "https:" || url.protocol === "http:" ? url.toString() : undefined;
  } catch {
    return undefined;
  }
}

function getInitials(value: string) {
  return value
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0])
    .join("")
    .toUpperCase();
}

function FriendLinkSkeleton() {
  return (
    <div
      aria-busy="true"
      aria-label="Loading links"
      className="grid gap-4 sm:grid-cols-2"
      role="status"
    >
      {Array.from({ length: 6 }, (_, index) => (
        <Card key={index} variant="secondary" className="min-h-44 gap-4 p-5">
          <div className="flex items-center gap-3">
            <Skeleton className="size-11 rounded-full" />
            <div className="space-y-2">
              <Skeleton className="h-4 w-28 rounded-lg" />
              <Skeleton className="h-3 w-36 rounded-lg" />
            </div>
          </div>
          <Skeleton className="h-4 w-full rounded-lg" />
          <Skeleton className="h-4 w-4/5 rounded-lg" />
        </Card>
      ))}
    </div>
  );
}

function FriendLinkCard({ link }: { link: FriendLinkResponse }) {
  const url = toSafeExternalUrl(link.url);
  const avatar = toSafeExternalUrl(link.avatar);

  if (!url) return null;

  return (
    <a
      className="group focus-visible:outline-accent block h-full rounded-xl focus-visible:outline-2 focus-visible:outline-offset-4"
      href={url}
      rel="noopener noreferrer"
      target="_blank"
    >
      <Card
        variant="secondary"
        className="group-hover:bg-surface-secondary h-full transition-colors"
      >
        <Card.Header className="flex-row items-center gap-3">
          <Avatar className="border-default-200 shrink-0 border" size="md" variant="soft">
            {avatar ? <Avatar.Image alt={`${link.name} avatar`} src={avatar} /> : null}
            <Avatar.Fallback>{getInitials(link.name) || "L"}</Avatar.Fallback>
          </Avatar>
          <div className="min-w-0 grow">
            <Card.Title className="truncate text-base">{link.name}</Card.Title>
            <Typography color="muted" type="body-xs" className="mt-1 truncate font-mono">
              {new URL(url).hostname}
            </Typography>
          </div>
          <ArrowUpRight
            aria-hidden="true"
            className="text-muted size-4 shrink-0 transition-transform group-hover:translate-x-0.5 group-hover:-translate-y-0.5"
          />
        </Card.Header>
        <Card.Content>
          <Typography color="muted" type="body-sm" className="line-clamp-3 leading-6">
            {link.description || "A fellow traveler worth visiting."}
          </Typography>
        </Card.Content>
      </Card>
    </a>
  );
}

function matchesFriendLink(link: FriendLinkResponse, query: string) {
  if (!query) return true;

  const url = toSafeExternalUrl(link.url);
  const searchable = [link.name, link.description, url ? new URL(url).hostname : ""]
    .filter(Boolean)
    .join(" ")
    .toLocaleLowerCase();

  return searchable.includes(query);
}

interface FriendLinksPageProps {
  compact?: boolean;
}

export function FriendLinksPage({ compact = false }: FriendLinksPageProps) {
  const { data: friendLinks = [], error, isLoading, refetch } = useGetPublicFriendLinksQuery();
  const [applyFriendLink, { isLoading: isApplying }] = useApplyFriendLinkMutation();
  const [application, setApplication] = useState<FriendLinkApplication>(EMPTY_APPLICATION);
  const [searchValue, setSearchValue] = useState("");
  const searchQuery = useDeferredValue(searchValue.trim().toLocaleLowerCase());
  const validLinks = friendLinks.filter((link) => Boolean(toSafeExternalUrl(link.url)));
  const visibleLinks = validLinks.filter((link) => matchesFriendLink(link, searchQuery));

  const updateApplication = <Key extends keyof FriendLinkApplication>(
    key: Key,
    value: FriendLinkApplication[Key]
  ) => {
    setApplication((current) => ({ ...current, [key]: value }));
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const name = application.name.trim();
    const url = toSafeExternalUrl(application.url);
    const email = application.email.trim();
    const avatar = application.avatar.trim();
    const description = application.description.trim();

    if (!name || !url || !email) {
      toast.warning("Add your site name, a valid URL, and a contact email.");
      return;
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      toast.warning("Enter a valid contact email.");
      return;
    }

    if (avatar && !toSafeExternalUrl(avatar)) {
      toast.warning("Avatar URLs must start with http:// or https://.");
      return;
    }

    try {
      await applyFriendLink({
        name,
        url,
        email,
        ...(avatar ? { avatar } : {}),
        ...(description ? { description } : {}),
      }).unwrap();
      setApplication(EMPTY_APPLICATION);
    } catch {
      // The mutation displays the API error through the shared toast helper.
    }
  };

  return (
    <div
      className={
        compact ? "w-full" : "bg-background min-h-[100dvh] w-full px-6 py-24 sm:px-10 sm:py-32"
      }
    >
      <div className={compact ? "w-full" : "mx-auto w-full max-w-6xl"}>
        {!compact ? (
          <header className="grid gap-10 lg:grid-cols-[minmax(0,1fr)_15rem] lg:items-end">
            <div className="max-w-3xl">
              <div className="text-muted flex items-center gap-2 font-mono text-xs font-semibold uppercase">
                <Globe aria-hidden="true" className="size-4" />
                The blogroll
              </div>
              <Typography type="h1" weight="bold" className="mt-5 leading-[1.02] text-balance">
                Good places lead to better ideas.
              </Typography>
              <Typography color="muted" type="body" className="mt-5 max-w-xl">
                A small collection of people and projects making thoughtful work on the open web.
              </Typography>
            </div>
            <div className="border-default-200 border-l pl-5 sm:pl-6">
              <Typography className="font-mono text-3xl tabular-nums" type="body">
                {validLinks.length.toLocaleString("en-US")}
              </Typography>
              <Typography color="muted" type="body-sm" className="mt-1">
                places to visit
              </Typography>
            </div>
          </header>
        ) : null}

        <section aria-label="Friend links" className={compact ? "" : "mt-14"}>
          {!isLoading && !error && validLinks.length > 0 ? (
            <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <SearchField
                className="w-full sm:max-w-sm"
                name="friend-link-search"
                value={searchValue}
                onChange={setSearchValue}
              >
                <Label className="sr-only">Search places to visit</Label>
                <SearchField.Group>
                  <SearchField.SearchIcon />
                  <SearchField.Input placeholder="Search the blogroll" />
                  <SearchField.ClearButton aria-label="Clear link search" />
                </SearchField.Group>
              </SearchField>
              <Typography aria-live="polite" color="muted" type="body-xs">
                {searchQuery
                  ? `${visibleLinks.length.toLocaleString("en-US")} matches`
                  : "Browse the blogroll"}
              </Typography>
            </div>
          ) : null}

          {isLoading ? <FriendLinkSkeleton /> : null}

          {!isLoading && error ? (
            <EmptyState size="lg">
              <EmptyState.Header>
                <EmptyState.Media variant="icon">
                  <CircleLink aria-hidden="true" />
                </EmptyState.Media>
                <EmptyState.Title>Links are unavailable</EmptyState.Title>
                <EmptyState.Description>
                  The blogroll could not be loaded. Please try again in a moment.
                </EmptyState.Description>
              </EmptyState.Header>
              <EmptyState.Content>
                <Button variant="outline" onPress={() => refetch()}>
                  <ArrowRotateLeft aria-hidden="true" />
                  Try again
                </Button>
              </EmptyState.Content>
            </EmptyState>
          ) : null}

          {!isLoading && !error && validLinks.length === 0 ? (
            <Card variant="secondary" className="p-0">
              <EmptyState size="lg">
                <EmptyState.Header>
                  <EmptyState.Media variant="icon">
                    <CircleLink aria-hidden="true" />
                  </EmptyState.Media>
                  <EmptyState.Title>No links published yet</EmptyState.Title>
                  <EmptyState.Description>
                    The first places on this list are being curated now. You can suggest yours
                    below.
                  </EmptyState.Description>
                </EmptyState.Header>
              </EmptyState>
            </Card>
          ) : null}

          {!isLoading && !error && validLinks.length > 0 && visibleLinks.length === 0 ? (
            <Card variant="secondary" className="p-0">
              <EmptyState size="md">
                <EmptyState.Header>
                  <EmptyState.Media variant="icon">
                    <CircleLink aria-hidden="true" />
                  </EmptyState.Media>
                  <EmptyState.Title>No places match your search</EmptyState.Title>
                  <EmptyState.Description>
                    Try a site name, a topic from its introduction, or a domain.
                  </EmptyState.Description>
                </EmptyState.Header>
                <EmptyState.Content>
                  <Button variant="secondary" onPress={() => setSearchValue("")}>
                    Clear search
                  </Button>
                </EmptyState.Content>
              </EmptyState>
            </Card>
          ) : null}

          {!isLoading && !error && visibleLinks.length > 0 ? (
            <div className="grid gap-4 sm:grid-cols-2">
              {visibleLinks.map((link) => (
                <FriendLinkCard key={link.id} link={link} />
              ))}
            </div>
          ) : null}
        </section>

        <section
          className={`border-default-200 grid gap-10 border-t pt-10 lg:grid-cols-[minmax(0,0.8fr)_minmax(23rem,0.7fr)] lg:gap-16 ${compact ? "mt-16" : "mt-20"}`}
        >
          <div className="max-w-xl">
            <div className="text-muted flex items-center gap-2 font-mono text-xs font-semibold uppercase">
              <CircleLink aria-hidden="true" className="size-4" />
              Link exchange
            </div>
            <Typography type="h2" weight="semibold" className="mt-4 text-balance">
              Add your corner of the web.
            </Typography>
            <Typography color="muted" type="body" className="mt-4 leading-7">
              Send a short introduction and a way to reach you. Each suggestion is reviewed before
              it appears here.
            </Typography>
            <Chip className="mt-6" size="sm" variant="soft">
              Reviewed before publishing
            </Chip>
          </div>

          <Card variant="secondary" className="p-5 sm:p-6">
            <Form className="gap-5" validationBehavior="native" onSubmit={handleSubmit}>
              <TextField
                fullWidth
                isRequired
                name="name"
                value={application.name}
                onChange={(value) => updateApplication("name", value)}
              >
                <Label>Site name</Label>
                <Input
                  maxLength={100}
                  placeholder="Your publication or project"
                  variant="secondary"
                />
                <FieldError />
              </TextField>

              <TextField
                fullWidth
                isRequired
                name="url"
                type="url"
                value={application.url}
                onChange={(value) => updateApplication("url", value)}
              >
                <Label>Site URL</Label>
                <Input maxLength={255} placeholder="https://example.com" variant="secondary" />
                <FieldError />
              </TextField>

              <TextField
                fullWidth
                isRequired
                name="email"
                type="email"
                value={application.email}
                onChange={(value) => updateApplication("email", value)}
              >
                <Label>Contact email</Label>
                <Input maxLength={100} placeholder="hello@example.com" variant="secondary" />
                <FieldError />
              </TextField>

              <TextField
                fullWidth
                name="avatar"
                type="url"
                value={application.avatar}
                onChange={(value) => updateApplication("avatar", value)}
              >
                <Label>
                  Avatar URL <span className="text-muted font-normal">(optional)</span>
                </Label>
                <Input
                  maxLength={255}
                  placeholder="https://example.com/avatar.png"
                  variant="secondary"
                />
                <FieldError />
              </TextField>

              <TextField
                fullWidth
                name="description"
                value={application.description}
                onChange={(value) => updateApplication("description", value)}
              >
                <Label>
                  Short introduction <span className="text-muted font-normal">(optional)</span>
                </Label>
                <TextArea
                  maxLength={500}
                  placeholder="What do you make or write about?"
                  rows={3}
                  variant="secondary"
                />
                <FieldError />
              </TextField>

              <Button isPending={isApplying} type="submit">
                <PaperPlane aria-hidden="true" className="size-4" />
                Submit for review
              </Button>
            </Form>
          </Card>
        </section>
      </div>
    </div>
  );
}
